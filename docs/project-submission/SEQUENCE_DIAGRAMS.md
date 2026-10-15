# AUREX — UML Sequence Diagrams Specification

**Document Version:** 1.0  
**Project Name:** AUREX — Financial Audit Risk Platform  
**Academic Context:** BCA Field Project, MIT Arts, Commerce & Science College, Alandi  
**Diagram Sources:**  
- [`diagrams/sequence-login.mmd`](diagrams/sequence-login.mmd)
- [`diagrams/sequence-risk-analysis.mmd`](diagrams/sequence-risk-analysis.mmd)
- [`diagrams/sequence-audit-decision.mmd`](diagrams/sequence-audit-decision.mmd)
- [`diagrams/sequence-csv-import.mmd`](diagrams/sequence-csv-import.mmd)

---

## 1. Sequence Diagram 1: Login & JWT Authentication

Models credential verification, password hash checking, token generation, and subsequent request authentication with live enabled-status validation.

```mermaid
sequenceDiagram
    autonumber
    actor User as Auditor / Admin (Browser)
    participant SPA as React Frontend (Vite)
    participant Filter as JwtAuthenticationFilter
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthService
    participant UserRepo as UserRepository
    participant Encoder as PasswordEncoder (BCrypt)
    participant JwtSvc as JwtService
    participant DB as PostgreSQL Database

    Note over User, SPA: 1. User submits login credentials
    User->>SPA: Enters username & password on /login
    SPA->>AuthCtrl: POST /api/auth/login {username, password}
    
    AuthCtrl->>AuthSvc: login(LoginRequest)
    AuthSvc->>UserRepo: findByUsername(username)
    UserRepo->>DB: SELECT id, username, password_hash, role, enabled... FROM users WHERE username = ?
    DB-->>UserRepo: ResultSet row
    UserRepo-->>AuthSvc: Optional<User>

    alt User Not Found or Account Disabled
        AuthSvc-->>AuthCtrl: throw BadCredentialsException / DisabledException
        AuthCtrl-->>SPA: 401 Unauthorized {"message": "Invalid username or password"}
        SPA-->>User: Display error message
    else User Exists & Enabled
        AuthSvc->>Encoder: matches(rawPassword, user.getPasswordHash())
        alt Password Mismatch
            Encoder-->>AuthSvc: false
            AuthSvc-->>AuthCtrl: throw BadCredentialsException
            AuthCtrl-->>SPA: 401 Unauthorized {"message": "Invalid username or password"}
        else Password Valid
            Encoder-->>AuthSvc: true
            AuthSvc->>JwtSvc: generateToken(username, userId, role)
            JwtSvc-->>AuthSvc: Signed JWT (HMAC-SHA256, 24h validity)
            AuthSvc-->>AuthCtrl: LoginResponse (token, tokenType, expiresIn, UserDto)
            AuthCtrl-->>SPA: 200 OK {token, user: {id, username, role, enabled}}
            SPA->>SPA: tokenStore.setToken(token), sync AuthContext
            SPA-->>User: Redirect to Dashboard (/)
        end
    end

    Note over User, DB: 2. Subsequent Authenticated API Call (e.g. GET /api/transactions)
    User->>SPA: Navigates to /transactions
    SPA->>Filter: GET /api/transactions (Header: "Authorization: Bearer <token>")
    Filter->>JwtSvc: validateToken(token)
    JwtSvc-->>Filter: true
    Filter->>JwtSvc: getUsernameFromToken(token)
    JwtSvc-->>Filter: "auditor_vighnesh"
    Filter->>UserRepo: findByUsername("auditor_vighnesh")
    UserRepo->>DB: SELECT ... FROM users WHERE username = ?
    DB-->>UserRepo: Live User row (checks enabled == true)
    Filter->>Filter: Build UsernamePasswordAuthenticationToken & set SecurityContext
    Filter->>SPA: Passes request down filter chain to Controller
```

---

## 2. Sequence Diagram 2: Deterministic Risk Analysis & Persistence

Models single-transaction risk evaluation, rule engine invocation across the 6 policies, mathematical score capping, severity categorization, and atomic persistence of the run and findings.

```mermaid
sequenceDiagram
    autonumber
    actor Auditor as Auditor (Browser)
    participant Ctrl as RiskController
    participant Svc as RiskAnalysisService
    participant Factory as RiskEngineFactory
    participant Engine as RiskEngine
    participant Calc as RiskScoreCalculator
    participant TxnRepo as TransactionRepository
    participant RunRepo as RiskAnalysisRunRepository
    participant FindingRepo as RiskFindingRepository
    participant DB as PostgreSQL Database

    Auditor->>Ctrl: POST /api/risk/analyze/{id} (Bearer JWT)
    Note over Ctrl: Security verifies ROLE_ADMIN or ROLE_AUDITOR
    Ctrl->>Svc: analyzeAndPersistTransaction(transactionId)
    
    rect rgb(240, 248, 255)
        Note over Svc, DB: @Transactional Boundary
        Svc->>TxnRepo: findAll()
        TxnRepo->>DB: SELECT transaction_id, vendor, employee, amount, transaction_time, category FROM transactions
        DB-->>TxnRepo: List<Transaction>
        TxnRepo-->>Svc: List<Transaction>
        
        alt Target Transaction Not Found
            Svc-->>Ctrl: throw TransactionNotFoundException
            Ctrl-->>Auditor: 404 Not Found {"error": "Transaction not found"}
        else Target Transaction Found
            Svc->>Factory: create()
            Factory-->>Svc: configured RiskEngine (6 Rules injected)
            
            Svc->>Engine: analyze(targetTxn, allTransactions)
            
            loop Evaluate Single-Transaction Policies
                Engine->>Engine: HighAmountRule.evaluate(targetTxn)
                Engine->>Engine: UnusualTimeRule.evaluate(targetTxn)
                Engine->>Engine: RoundAmountRule.evaluate(targetTxn)
            end
            
            loop Evaluate Dataset-Aware Policies
                Engine->>Engine: DuplicateTransactionRule.evaluate(targetTxn, allTransactions)
                Engine->>Engine: TransactionVelocityRule.evaluate(targetTxn, allTransactions)
                Engine->>Engine: VendorConcentrationRule.evaluate(targetTxn, allTransactions)
            end
            
            Engine->>Calc: calculateScore(findings)
            Calc-->>Engine: capped total score (0 - 100)
            Engine->>Calc: determineSeverity(totalScore)
            Calc-->>Engine: RiskSeverity (LOW, MEDIUM, HIGH, CRITICAL)
            Engine-->>Svc: RiskReport (score, severity, List<RiskFinding>)
            
            Svc->>RunRepo: save(transactionId, score, severity)
            RunRepo->>DB: INSERT INTO risk_analysis_runs (transaction_id, risk_score, risk_level) VALUES (?, ?, ?) RETURNING id
            DB-->>RunRepo: generated analysis_run_id (e.g. 1042)
            RunRepo-->>Svc: 1042
            
            loop For Each Triggered Finding
                Svc->>FindingRepo: save(1042, transactionId, finding)
                FindingRepo->>DB: INSERT INTO risk_findings (analysis_run_id, transaction_id, risk_type, score, severity, explanation) VALUES (?, ?, ?, ?, ?, ?)
                DB-->>FindingRepo: Success
            end
            
            Svc-->>Ctrl: RiskReport
        end
    end
    Ctrl-->>Auditor: 200 OK RiskReport {riskScore, riskLevel, findings: [...]}
```

---

## 3. Sequence Diagram 3: Recording an Audit Decision & Audit Event

Models submission of formal auditor determinations, principal identity enforcement, decision persistence, and atomic dual-write (decision + event) within a Spring `@Transactional` boundary. The transaction guarantees atomicity — either both rows are committed or neither is — but does not provide cryptographic immutability or tamper-proof storage.

```mermaid
sequenceDiagram
    autonumber
    actor Auditor as Authenticated Auditor (Principal)
    participant Ctrl as AuditController
    participant Svc as AuditService
    participant TxnRepo as TransactionRepository
    participant RunRepo as RiskAnalysisRunRepository
    participant DecisionRepo as AuditDecisionRepository
    participant EventRepo as AuditEventRepository
    participant DB as PostgreSQL Database

    Auditor->>Ctrl: POST /api/risk/transactions/{id}/audit/decisions<br/>Body: {decision: "REQUIRES_INVESTIGATION", comment: "...", analysisRunId: 1042}
    Note over Ctrl: Security verifies ROLE_ADMIN or ROLE_AUDITOR
    
    Ctrl->>Ctrl: Extract actor = principal.getName().trim()<br/>(Ignores client-supplied decidedBy)
    Ctrl->>Svc: recordDecision(transactionId, request, actor)
    
    rect rgb(240, 248, 255)
        Note over Svc, DB: @Transactional Boundary
        Svc->>TxnRepo: findById(transactionId)
        TxnRepo->>DB: SELECT * FROM transactions WHERE transaction_id = ?
        DB-->>TxnRepo: Transaction row
        TxnRepo-->>Svc: Transaction
        
        alt Target Analysis Run Specified
            Svc->>RunRepo: findByIdAndTransactionId(analysisRunId, transactionId)
            RunRepo->>DB: SELECT * FROM risk_analysis_runs WHERE id = ? AND transaction_id = ?
            DB-->>RunRepo: RiskAnalysisRun row
            RunRepo-->>Svc: RiskAnalysisRun
        else No Analysis Run ID in Request
            Svc->>RunRepo: findLatestByTransactionId(transactionId)
            RunRepo->>DB: SELECT * FROM risk_analysis_runs WHERE transaction_id = ? ORDER BY analyzed_at DESC, id DESC LIMIT 1
            DB-->>RunRepo: Latest RiskAnalysisRun
            RunRepo-->>Svc: Latest RiskAnalysisRun
        end
        
        Svc->>DecisionRepo: save(transactionId, runId, decision, comment, actor)
        DecisionRepo->>DB: INSERT INTO audit_decisions (transaction_id, analysis_run_id, decision, comment, decided_by) VALUES (?, ?, ?, ?, ?) RETURNING id
        DB-->>DecisionRepo: decisionId (e.g. 501)
        DecisionRepo-->>Svc: 501
        
        Svc->>EventRepo: save(transactionId, decisionId, "AUDIT_DECISION_CREATED", eventDetails, actor)
        EventRepo->>DB: INSERT INTO audit_events (transaction_id, decision_id, event_type, event_details, actor) VALUES (?, ?, ?, ?, ?) RETURNING id
        DB-->>EventRepo: eventId (e.g. 901)
        EventRepo-->>Svc: 901
        
        Svc->>DecisionRepo: findById(501)
        DecisionRepo->>DB: SELECT * FROM audit_decisions WHERE id = 501
        DB-->>DecisionRepo: Full AuditDecision record
        DecisionRepo-->>Svc: AuditDecision
    end
    
    Svc-->>Ctrl: AuditDecision
    Ctrl-->>Auditor: 201 Created AuditDecision {id: 501, transactionId, decision, comment, decidedBy, decidedAt}
```

---

## 4. Sequence Diagram 4: Multipart CSV Ingestion Workflow

Models multipart CSV reception, temporary file disk streaming, RFC 4180 parsing, manual JDBC transactional batch insertion (`connection.setAutoCommit(false)`), rollback handling, and temp-file cleanup.

```mermaid
sequenceDiagram
    autonumber
    actor Auditor as Auditor / Admin
    participant Ctrl as TransactionImportController
    participant OS as Local File System (Temp Storage)
    participant Facade as TransactionImportFacade
    participant Svc as TransactionImportService
    participant Reader as TransactionCsvReader
    participant TxnRepo as TransactionRepository
    participant DB as PostgreSQL Database

    Auditor->>Ctrl: POST /api/transactions/import (Multipart: "file"=.csv, Bearer JWT)
    Note over Ctrl: Security verifies ROLE_ADMIN or ROLE_AUDITOR
    
    alt File Empty or Missing
        Ctrl-->>Auditor: 400 Bad Request {"message": "Uploaded file is missing or empty"}
    else File Extension Not .csv
        Ctrl-->>Auditor: 400 Bad Request {"message": "Only CSV files are accepted (.csv extension required)"}
    else Valid CSV Upload
        Ctrl->>OS: Files.createTempFile("aurex-import-", ".csv")
        OS-->>Ctrl: Path tempFile
        Ctrl->>OS: file.transferTo(tempFile)
        
        Ctrl->>Facade: importFromCsv(tempFilePath)
        Facade->>Svc: importFromCsv(tempFilePath)
        
        Svc->>Reader: read(tempFilePath)
        Reader->>OS: Read and parse CSV lines (RFC 4180)
        OS-->>Reader: Line records
        Reader-->>Svc: List<Transaction> (Parsed objects)
        
        rect rgb(240, 248, 255)
            Note over Svc, DB: Manual Transactional Control (Plain JDBC)
            Svc->>DB: dataSource.getConnection()
            DB-->>Svc: Connection conn
            Svc->>Svc: conn.setAutoCommit(false)
            
            loop For Each Parsed Transaction
                Svc->>TxnRepo: save(conn, transaction)
                TxnRepo->>DB: INSERT INTO transactions (transaction_id, vendor, employee, amount, transaction_time, category) VALUES (?, ?, ?, ?, ?, ?)
                DB-->>TxnRepo: 1 row affected
            end
            
            alt Any Constraint Violation or Duplicate Key
                Svc->>Svc: conn.rollback()
                Svc-->>Facade: throw SQLException
                Facade-->>Ctrl: throw Exception
                Ctrl-->>Auditor: 400 Bad Request {"message": "Import failed: ..."}
            else All Transactions Saved Successfully
                Svc->>Svc: conn.commit()
                Svc-->>Facade: importedCount
                Facade-->>Ctrl: importedCount
            end
        end
        
        Ctrl->>OS: Files.deleteIfExists(tempFile)
        Ctrl-->>Auditor: 200 OK ImportResponse {"message": "Transactions imported successfully", "importedCount": N}
    end
```

---

## 5. Sequence Diagram 5: Administrative User Management & Lockout Protection

Models user creation and account status toggling, including the execution of self-disable protection and pessimistic row-locking last-admin lockout prevention.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Compliance Admin
    participant Ctrl as UserController
    participant Svc as UserService
    participant Repo as UserRepository
    participant Encoder as PasswordEncoder (BCrypt)
    participant DB as PostgreSQL Database

    Note over Admin, DB: 1. Provisioning a New User Account
    Admin->>Ctrl: POST /api/users {username: "new_auditor", password: "...", role: "AUDITOR"}
    Note over Ctrl: Security verifies ROLE_ADMIN
    Ctrl->>Svc: createUser(CreateUserRequest)
    Svc->>Repo: existsByUsername("new_auditor")
    Repo->>DB: SELECT COUNT(*) FROM users WHERE username = ?
    DB-->>Repo: 0
    Repo-->>Svc: false
    
    Svc->>Encoder: encode(rawPassword)
    Encoder-->>Svc: "$2a$10$e8..." (BCrypt hash)
    Svc->>Repo: save(user)
    Repo->>DB: INSERT INTO users (username, password_hash, role, enabled) VALUES (?, ?, ?, ?)
    DB-->>Repo: Generated ID 15
    Repo-->>Svc: User instance
    Svc-->>Ctrl: UserDto (password fields omitted)
    Ctrl-->>Admin: 201 Created UserDto

    Note over Admin, DB: 2. Toggling User Status with Lockout Guard
    Admin->>Ctrl: PATCH /api/users/{id}/status {enabled: false}
    Ctrl->>Svc: updateUserStatus(targetId, false, adminUsername)
    Svc->>Repo: findById(targetId)
    Repo->>DB: SELECT * FROM users WHERE id = ?
    DB-->>Repo: Target User record
    Repo-->>Svc: User targetUser

    alt Target User is Currently Authenticated Admin (Self-Disable)
        Svc-->>Ctrl: throw UserConflictException("Cannot disable your own authenticated account")
        Ctrl-->>Admin: 409 Conflict {"message": "Cannot disable your own authenticated account"}
    else Target User is an ADMIN
        Svc->>Repo: findAdminsForUpdate()
        Repo->>DB: SELECT * FROM users WHERE role = 'ADMIN' ORDER BY id ASC FOR UPDATE
        Note over DB: Locks all ADMIN rows in transaction
        DB-->>Repo: List<User> adminRows
        Repo-->>Svc: List<User> adminRows
        
        alt Count of Enabled Admins <= 1
            Svc-->>Ctrl: throw UserConflictException("Cannot disable the last enabled ADMIN account")
            Ctrl-->>Admin: 409 Conflict {"message": "Cannot disable the last enabled ADMIN account"}
        else Multiple Enabled Admins Exist
            Svc->>Repo: updateStatus(targetId, false)
            Repo->>DB: UPDATE users SET enabled = false, updated_at = NOW() WHERE id = ?
            DB-->>Repo: 1 row updated
            Svc-->>Ctrl: Updated UserDto
            Ctrl-->>Admin: 200 OK UserDto {enabled: false}
        end
    end
```
