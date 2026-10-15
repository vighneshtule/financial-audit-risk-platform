# AUREX — Repository Findings & Technical Discovery Report

**Project Name:** AUREX — Financial Audit Risk Platform  
**Repository:** [https://github.com/vighnesh/financial-audit-risk-platform](https://github.com/vighnesh/financial-audit-risk-platform)  
**Local Workspace:** `E:\Vighnesh\financial-audit-risk-platform`  
**Academic Context:** BCA Field Project, MIT Arts, Commerce & Science College, Alandi  
**Inspection Date:** October 2026  
**Git Branch & Status:** `main` (clean working tree, up to date with `origin/main`)

---

## 1. Executive Technical Summary

An in-depth, code-level static inspection and runtime test verification was conducted across the active repository. Every module—including the Spring Boot backend, plain JDBC persistence layer, React/TypeScript single-page frontend, Docker orchestration, and automated test suites—was analyzed directly against the codebase.

The system is fully functional and deterministically sound. It implements a financial audit and risk-intelligence platform that combines high-performance relational database storage (PostgreSQL 16) with plain JDBC (no JPA/Hibernate overhead), a modular 6-rule deterministic audit engine, JWT-based role-based access control (RBAC), and forensic audit decision logging.

---

## 2. Verified Technology Stack

| Architectural Layer | Technology / Framework | Verified Version | Verification Evidence & Location |
|---|---|---|---|
| **Runtime Environment** | Java OpenJDK | `17.0.19 LTS` (Microsoft) | Verified via `java -version` and `pom.xml` (`<maven.compiler.source>17</maven.compiler.source>`) |
| **Backend Framework** | Spring Boot | `3.5.6` | Verified in `pom.xml` parent artifact: `org.springframework.boot:spring-boot-starter-parent:3.5.6` |
| **Security & Auth** | Spring Security + JJWT | Spring Security 6.x / JJWT `0.12.6` | `io.jsonwebtoken:jjwt-api:0.12.6`, `JwtAuthenticationFilter.java`, `SecurityConfig.java` |
| **Persistence Layer** | Plain JDBC (`JdbcTemplate` & `DataSource`) | Spring JDBC 6.x | **No JPA / Hibernate dependencies present**. Uses `JdbcTemplate`, `DataSourceUtils`, raw `PreparedStatement`, `ResultSet` |
| **Database Engine** | PostgreSQL | `16` (Driver: `42.7.8`) | `org.postgresql:postgresql:42.7.8`, `schema.sql`, `docker-compose.yml` (`postgres:16-alpine`) |
| **API Documentation** | Springdoc OpenAPI / Swagger UI | `2.8.5` | `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5`, `/swagger-ui.html` |
| **Backend Testing** | JUnit 5 + Testcontainers | JUnit `5.12.2`, Testcontainers `1.21.4` | `org.testcontainers:postgresql:1.21.4`, Spring Boot Test, MockMvc |
| **Frontend Framework** | React + TypeScript | React `19.2.8`, TypeScript `6.0.2` | `frontend/package.json`, `tsconfig.json` |
| **Frontend Build Tool** | Vite | `8.3.0` / `8.3.1` | `frontend/package.json`, `frontend/vite.config.ts` |
| **Frontend Styling** | Tailwind CSS (Vite plugin) | `4.3.3` | `@tailwindcss/vite:4.3.3`, `tailwindcss:4.3.3`, `frontend/src/index.css` |
| **Routing & UI** | React Router DOM + Framer Motion + Lucide | Router `7.18.4`, Motion `13.4.3`, Lucide `1.48.0` | `frontend/package.json`, `frontend/src/App.tsx` |
| **Data Visualization** | Recharts | `3.10.1` | `frontend/package.json`, `frontend/src/pages/Dashboard.tsx`, `RiskIntelligence.tsx` |
| **Frontend Testing** | Node.js Test Runner | Node `v24.11.1` built-in test runner | `node --test src/__tests__/*.test.mjs`, `frontend/package.json` |
| **Deployment & Gateway** | Docker Compose + Nginx | PostgreSQL 16 Alpine, Temurin 17 Alpine, Nginx Alpine | `docker-compose.yml`, `Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf` |

---

## 3. Actual Feature Inventory

### 3.1 Implemented & Fully Verified
1. **User Authentication & Session Management**:
   - Database-backed user authentication (`POST /api/auth/login`) with BCrypt salted password hashing.
   - Stateless JWT generation (HMAC-SHA256, 24-hour default expiration) storing user identity, ID, and role claim.
   - Dynamic per-request user status validation in `JwtAuthenticationFilter`: disabled accounts are immediately refused with HTTP 401 even if their JWT is structurally valid.
   - Current user inspection (`GET /api/auth/me`).
   - Graceful session expiry handling on frontend with automatic token clearance and redirect.
2. **Administrative User Management (RBAC)**:
   - Full user listing (`GET /api/users`), user creation (`POST /api/users`), and status enabling/disabling (`PATCH /api/users/{id}/status`).
   - Self-disable protection: Admins cannot disable their own active account.
   - Last-admin lockout protection: The last active `ADMIN` account cannot be disabled (enforced with pessimistic row locking `SELECT ... FOR UPDATE` in PostgreSQL).
   - Dedicated frontend Admin Users view with status badges, confirmation modals, and role filters.
3. **Transaction Ingestion**:
   - Multi-row CSV file upload (`POST /api/transactions/import`) with RFC 4180 parsing in `TransactionCsvReader`.
   - Atomic database batch insertion inside a single caller-managed JDBC transaction.
   - System-wide transaction retrieval (`GET /api/transactions`).
4. **Deterministic Risk Evaluation Engine**:
   - Single-transaction and cross-dataset evaluation across 6 independent audit policies.
   - Real-time on-the-fly analysis (`GET /api/risk/transactions/{id}`) and batch evaluation (`GET /api/risk/transactions`).
   - Historical persistence of analysis runs (`POST /api/risk/analyze/{id}`) and complete dataset batch runs (`POST /api/risk/analyze-all`).
   - Paginated historical runs comparison per transaction (`GET /api/risk/transactions/{id}/history/page`).
5. **Cross-Entity Evidence & Relationship Intelligence**:
   - Forensic context graph querying related vendor, employee, and category transactions (`GET /api/risk/transactions/{id}/context`).
   - Computes peer transaction volume, aggregate monetary value, and peak historical risk score.
6. **Forensic Audit Decision & Append-Only Event History**:
   - Audit decision recording (`POST /api/risk/transactions/{transactionId}/audit/decisions`) attaching to specific or latest risk runs.
   - Actor integrity protection: `decided_by` and event `actor` are derived strictly from `Principal.getName()`, preventing client tampering.
   - Dual-table persistence: inserts decision row in `audit_decisions` and an audit event log in `audit_events` within a `@Transactional` boundary.
   - Decision and event history retrieval endpoints (`GET .../decisions`, `GET .../decision`, `GET .../events`).
7. **Risk Intelligence Multi-Dimensional Analytics**:
   - Aggregated metrics across dataset overview, severity counts, vendor concentration, employee risk profiling, category risk distribution, and risk type contributions (`GET /api/risk/intelligence/summary` and `GET /api/risk/summary`).
8. **System Health Endpoint**:
   - Public health check (`GET /api/health`) verifying live database connectivity.

### 3.2 Unimplemented / Roadmap Features (Found in Readme Roadmap but NOT in Source Code)
- **Generative AI / LLM Narrative Summary**: Readme mentions an "AI-Assisted Investigation Assistant". No LLM API calls, LangChain, OpenAI, or Ollama integrations exist in code.
- **Workflow State Engine for Ticket Cases**: Outside of the audit decision status values (`CONFIRMED_RISK`, `FALSE_POSITIVE`, `REQUIRES_INVESTIGATION`, `ESCALATED`), there is no standalone Jira/ticket-style case lifecycle engine (`OPEN`, `IN_REVIEW`, `RESOLVED`).

---

## 4. Key Package Structure & Responsibilities

```
src/main/java/
├── Main.java                                   # Legacy console launcher (historical entry point)
├── com/vighnesh/
│   ├── FinancialAuditRiskApplication.java       # Primary Spring Boot Application Entry Point
│   ├── auth/
│   │   ├── AuthController.java                 # Login and /me endpoints
│   │   ├── AuthService.java                    # Authentication service (BCrypt verify & JWT issuance)
│   │   ├── LoginRequest.java                   # Login payload DTO (username, password)
│   │   └── LoginResponse.java                  # Login response DTO (token, tokenType, expiresIn, user)
│   ├── config/
│   │   ├── OpenApiConfig.java                  # Swagger / OpenAPI 3.0 configuration
│   │   └── WebConfig.java                      # CORS and MVC configuration
│   ├── controller/
│   │   ├── AuditController.java                # Audit decision & audit event REST endpoints
│   │   ├── HealthController.java               # Public database health check endpoint
│   │   ├── ImportResponse.java                 # CSV upload response DTO
│   │   ├── RiskController.java                 # Risk analysis, findings, history & pagination
│   │   ├── RiskIntelligenceController.java     # Aggregate multidimensional analytics endpoint
│   │   ├── TransactionController.java          # Raw transaction queries
│   │   └── TransactionImportController.java    # Multipart CSV file upload controller
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java         # Centralized @RestControllerAdvice error mapping
│   │   ├── AnalysisRunNotFoundException.java   # HTTP 404 mapping
│   │   ├── TransactionNotFoundException.java   # HTTP 404 mapping
│   │   ├── UserConflictException.java          # HTTP 409 mapping
│   │   └── UserNotFoundException.java          # HTTP 404 mapping
│   ├── security/
│   │   ├── CustomAccessDeniedHandler.java      # HTTP 403 Forbidden JSON formatter
│   │   ├── CustomAuthenticationEntryPoint.java # HTTP 401 Unauthorized JSON formatter
│   │   ├── CustomUserDetailsService.java       # Bridge between Spring Security and UserRepository
│   │   ├── DevAdminBootstrap.java              # Development profile admin seeder
│   │   ├── JwtAuthenticationFilter.java        # Per-request JWT parser and live enabled-flag validator
│   │   ├── JwtService.java                     # HMAC-SHA256 signer, parser and validator
│   │   ├── ProdAdminBootstrap.java             # Production first-boot admin provisioning
│   │   └── SecurityConfig.java                 # WebSecurityConfigurerAdapter replacement (Filter Chain)
│   ├── service/
│   │   ├── AnalyticsService.java               # Service facade for multidimensional metrics
│   │   ├── AuditService.java                   # Transactional audit decision & event manager
│   │   ├── RiskAnalysisService.java            # Risk engine orchestrator, persistence & pagination
│   │   ├── RiskEngineFactory.java              # Factory assembling RiskEngine with configured rules
│   │   ├── RiskEvidenceService.java            # Entity context graph assembler
│   │   ├── RiskScoreCalculator.java            # Capped score aggregator & severity mapper
│   │   └── TransactionImportFacade.java        # Spring facade wrapping TransactionImportService
│   └── user/
│       ├── CreateUserRequest.java              # New user DTO with JSR-380 validation
│       ├── UpdateUserStatusRequest.java        # Enable/disable boolean request DTO
│       ├── User.java                           # User domain entity
│       ├── UserController.java                 # Admin user management REST controller
│       ├── UserDto.java                        # User response DTO (password sanitized)
│       ├── UserRepository.java                 # Plain JDBC user repository (JdbcTemplate)
│       ├── UserRole.java                       # Enum: ADMIN, AUDITOR, VIEWER
│       └── UserService.java                    # Business logic for user lifecycle & protection rules
├── config/
│   ├── DatabaseConfig.java                     # DataSource bean provider
│   ├── DatabaseConnection.java                 # Legacy direct DriverManager fallback
│   └── RiskConfiguration.java                  # @ConfigurationProperties binding risk thresholds
├── model/                                      # Domain models, enums and query projections
├── repository/                                 # Plain JDBC repositories with raw SQL
├── rule/                                       # Independent deterministic risk rules
└── service/                                    # Plain-Java core risk engine & CSV parser
```

---

## 5. Actual Database Tables & Relationships

PostgreSQL schema initialization script: [`src/main/resources/schema.sql`](../../src/main/resources/schema.sql).

### Table Summary

```
                      ┌──────────────────────┐
                      │        users         │
                      └──────────────────────┘

                      ┌──────────────────────┐
                      │     transactions     │
                      └──────────┬───────────┘
                                 │ 1
                                 │
                                 │ * (ON DELETE CASCADE)
                      ┌──────────▼───────────┐
                      │  risk_analysis_runs  │
                      └────┬────────────┬────┘
                           │ 1          │ 1
     (ON DELETE CASCADE)   │            │   (ON DELETE RESTRICT)
             ┌─────────────┘            └─────────────┐
             │ *                                      │ *
  ┌──────────▼───────────┐                 ┌──────────▼───────────┐
  │    risk_findings     │                 │   audit_decisions    │
  └──────────────────────┘                 └──────────┬───────────┘
                                                      │ 1
                                                      │
                                                      │ * (ON DELETE RESTRICT)
                                           ┌──────────▼───────────┐
                                           │     audit_events     │
                                           └──────────────────────┘
```

1. **`users`**:
   - `id` (BIGSERIAL, Primary Key)
   - `username` (VARCHAR(100), NOT NULL, UNIQUE, Index: `idx_users_username`)
   - `password_hash` (VARCHAR(255), NOT NULL)
   - `role` (VARCHAR(20), NOT NULL, Check Constraint: `role IN ('ADMIN', 'AUDITOR', 'VIEWER')`, Index: `idx_users_role`)
   - `enabled` (BOOLEAN, NOT NULL, DEFAULT TRUE)
   - `created_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
   - `updated_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
2. **`transactions`**:
   - `transaction_id` (VARCHAR(50), Primary Key)
   - `vendor` (VARCHAR(255), NOT NULL, Index: `idx_transactions_vendor`)
   - `employee` (VARCHAR(100), NOT NULL, Index: `idx_transactions_employee`)
   - `amount` (NUMERIC(15,2), NOT NULL)
   - `transaction_time` (TIMESTAMP, NOT NULL, Index: `idx_transactions_time`)
   - `category` (VARCHAR(100), NOT NULL, Index: `idx_transactions_category`)
3. **`risk_analysis_runs`**:
   - `id` (BIGSERIAL, Primary Key)
   - `transaction_id` (VARCHAR(50), NOT NULL, FK → `transactions(transaction_id)` ON DELETE CASCADE, Index: `idx_analysis_runs_transaction`)
   - `risk_score` (INTEGER, NOT NULL)
   - `risk_level` (VARCHAR(20), NOT NULL)
   - `analyzed_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP, Index: `idx_analysis_runs_analyzed_at`)
   - Composite Index: `idx_analysis_runs_txn_analyzed` (`transaction_id`, `analyzed_at DESC`, `id DESC`) for fast latest run lookup.
4. **`risk_findings`**:
   - `id` (BIGSERIAL, Primary Key)
   - `analysis_run_id` (BIGINT, NOT NULL, FK → `risk_analysis_runs(id)` ON DELETE CASCADE, Index: `idx_risk_findings_analysis_run`)
   - `transaction_id` (VARCHAR(50), NOT NULL, FK → `transactions(transaction_id)` ON DELETE CASCADE, Index: `idx_risk_findings_transaction`)
   - `risk_type` (VARCHAR(100), NOT NULL, Index: `idx_risk_findings_type`)
   - `score` (INTEGER, NOT NULL)
   - `severity` (VARCHAR(20), NOT NULL, Index: `idx_risk_findings_severity`)
   - `explanation` (TEXT, NOT NULL)
   - `created_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP)
5. **`audit_decisions`**:
   - `id` (BIGSERIAL, Primary Key)
   - `transaction_id` (VARCHAR(50), NOT NULL, FK → `transactions(transaction_id)` ON DELETE RESTRICT, Index: `idx_audit_decisions_transaction`)
   - `analysis_run_id` (BIGINT, NOT NULL, FK → `risk_analysis_runs(id)` ON DELETE RESTRICT, Index: `idx_audit_decisions_analysis_run`)
   - `decision` (VARCHAR(40), NOT NULL) — values correspond to `AuditDecisionType`: `CONFIRMED_RISK`, `FALSE_POSITIVE`, `REQUIRES_INVESTIGATION`, `ESCALATED`
   - `comment` (TEXT)
   - `decided_by` (VARCHAR(100))
   - `decided_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP, Composite Index: `idx_audit_decisions_decided_at`)
6. **`audit_events`**:
   - `id` (BIGSERIAL, Primary Key)
   - `transaction_id` (VARCHAR(50), NOT NULL, FK → `transactions(transaction_id)` ON DELETE RESTRICT, Index: `idx_audit_events_transaction`)
   - `decision_id` (BIGINT, Nullable, FK → `audit_decisions(id)` ON DELETE RESTRICT, Index: `idx_audit_events_decision`)
   - `event_type` (VARCHAR(50), NOT NULL)
   - `event_details` (TEXT)
   - `actor` (VARCHAR(100))
   - `created_at` (TIMESTAMP, NOT NULL, DEFAULT CURRENT_TIMESTAMP, Composite Index: `idx_audit_events_created_at`)

---

## 6. Complete API Endpoint Inventory

| HTTP Method | Path | Controller Method | Auth Required | Required Role | Request Body / Params | Response Codes | Response Body Shape |
|---|---|---|---|---|---|---|---|
| `POST` | `/api/auth/login` | `AuthController.login` | No (PermitAll) | None | JSON: `LoginRequest` | 200, 400, 401 | `LoginResponse` (`token`, `tokenType`, `expiresIn`, `user`) |
| `GET` | `/api/auth/me` | `AuthController.me` | Yes | Any Authenticated | Header: `Bearer <jwt>` | 200, 401 | `UserDto` (`id`, `username`, `role`, `enabled`, timestamps) |
| `GET` | `/api/health` | `HealthController.checkHealth` | No (PermitAll) | None | None | 200, 503 | JSON: `{"status":"UP","database":"UP"}` |
| `GET` | `/api/users` | `UserController.getAllUsers` | Yes | `ADMIN` | None | 200, 401, 403 | `List<UserDto>` |
| `POST` | `/api/users` | `UserController.createUser` | Yes | `ADMIN` | JSON: `CreateUserRequest` | 201, 400, 401, 403, 409 | `UserDto` |
| `PATCH` | `/api/users/{id}/status` | `UserController.updateUserStatus` | Yes | `ADMIN` | JSON: `UpdateUserStatusRequest` | 200, 400, 401, 403, 404, 409 | `UserDto` |
| `GET` | `/api/transactions` | `TransactionController.getAllTransactions` | Yes | Any Authenticated | None | 200, 401 | `List<Transaction>` |
| `POST` | `/api/transactions/import` | `TransactionImportController.importCsv` | Yes | `ADMIN`, `AUDITOR` | Multipart: `file` (.csv) | 200, 400, 401, 403 | `ImportResponse` (`message`, `importedCount`) |
| `GET` | `/api/risk/summary` | `RiskController.getRiskSummary` | Yes | Any Authenticated | None | 200, 401 | `RiskSummary` |
| `GET` | `/api/risk/transactions` | `RiskController.getRiskTransactions` | Yes | Any Authenticated | Query: `riskLevel`, `minScore`, `page`, `size` | 200, 400, 401 | `RiskTransactionPage` |
| `GET` | `/api/risk/transactions/{id}` | `RiskController.analyzeTransaction` | Yes | Any Authenticated | Path: `id` | 200, 401, 404 | `RiskReport` (`riskScore`, `riskLevel`, `findings`) |
| `GET` | `/api/risk/transactions/{id}/findings` | `RiskController.getPersistedFindings` | Yes | Any Authenticated | Path: `id` | 200, 401, 404 | `List<RiskFinding>` |
| `GET` | `/api/risk/transactions/{id}/history` | `RiskController.getTransactionRiskHistory` | Yes | Any Authenticated | Path: `id` | 200, 401, 404 | `RiskAnalysisHistoryResponse` |
| `GET` | `/api/risk/transactions/{id}/history/{runId}` | `RiskController.getTransactionRiskHistoryRun` | Yes | Any Authenticated | Path: `id`, `runId` | 200, 401, 404 | `RiskAnalysisHistoryItem` |
| `GET` | `/api/risk/transactions/{id}/history/page` | `RiskController.getTransactionRiskHistoryPage` | Yes | Any Authenticated | Query: `page`, `size` | 200, 400, 401, 404 | `RiskAnalysisHistoryPage` |
| `GET` | `/api/risk/transactions/{id}/context` | `RiskController.getTransactionContext` | Yes | Any Authenticated | Path: `id` | 200, 401, 404 | `TransactionContext` (vendor, employee, category groups) |
| `POST` | `/api/risk/analyze/{id}` | `RiskController.analyzeAndPersistTransaction` | Yes | `ADMIN`, `AUDITOR` | Path: `id` | 200, 401, 403, 404 | `RiskReport` |
| `POST` | `/api/risk/analyze-all` | `RiskController.analyzeAll` | Yes | `ADMIN`, `AUDITOR` | None | 200, 401, 403 | `AnalysisResult` (batch statistics) |
| `GET` | `/api/risk/intelligence/summary` | `RiskIntelligenceController.getSummary` | Yes | Any Authenticated | None | 200, 401 | `RiskIntelligenceSummary` |
| `POST` | `/api/risk/transactions/{id}/audit/decisions` | `AuditController.createDecision` | Yes | `ADMIN`, `AUDITOR` | JSON: `CreateAuditDecisionRequest` | 201, 400, 401, 403, 404 | `AuditDecision` |
| `GET` | `/api/risk/transactions/{id}/audit/decisions` | `AuditController.getDecisions` | Yes | Any Authenticated | Path: `id` | 200, 401, 404 | `List<AuditDecision>` |
| `GET` | `/api/risk/transactions/{id}/audit/decision` | `AuditController.getLatestDecision` | Yes | Any Authenticated | Path: `id` | 200, 401, 404 | `AuditDecision` |
| `GET` | `/api/risk/transactions/{id}/audit/events` | `AuditController.getAuditEvents` | Yes | Any Authenticated | Path: `id` | 200, 401, 404 | `List<AuditEvent>` |
| `GET` | `/v3/api-docs` | Springdoc OpenAPI | No (PermitAll) | None | None | 200 | OpenAPI 3.0 JSON Spec |
| `GET` | `/swagger-ui.html` | Springdoc Swagger UI | No (PermitAll) | None | None | 200, 302 | Interactive Swagger UI |

---

## 7. Deterministic Risk Engine: Policies & Exact Implemented Thresholds

All rules implement either `RiskRule` (single-transaction) or `DatasetRiskRule` (cross-dataset context). Configured properties bind via `@ConfigurationProperties(prefix = "risk.rules")` in `RiskConfiguration.java`:

| Rule Name | Rule Interface | Target Risk Type | Default Threshold / Condition | Default Score | Finding Severity | Explanation Pattern |
|---|---|---|---|---|---|---|
| **High Amount Rule** | `RiskRule` | `HIGH_AMOUNT` | Transaction amount `> 100,000` INR | `+30` | `MEDIUM` | `"Transaction amount exceeds configured threshold of INR 100000"` |
| **Unusual Time Rule** | `RiskRule` | `UNUSUAL_TRANSACTION_TIME` | Transaction time `< 09:00` or `> 18:00` | `+20` | `MEDIUM` | `"Transaction occurred outside normal business hours"` |
| **Round Amount Rule** | `RiskRule` | `ROUND_AMOUNT` | `amount > 0` and `amount % 10,000 == 0` | `+10` | `MEDIUM` | `"Transaction amount is a round multiple of INR 10000"` |
| **Transaction Velocity Rule** | `DatasetRiskRule` | `TRANSACTION_VELOCITY` | Same employee has `>= 3` transactions within a `30`-minute sliding window (`Math.abs(Δt) <= 30m`) | `+20` | `MEDIUM` | `"Employee has 3 or more transactions within a 30-minute window"` |
| **Duplicate Transaction Rule** | `DatasetRiskRule` | `DUPLICATE_TRANSACTION` | Identical `(vendor, employee, amount, category)` with different ID within `10` minutes (`Math.abs(Δt) <= 10m`) | `+25` | `MEDIUM` | `"Possible duplicate transaction detected"` |
| **Vendor Concentration Rule** | `DatasetRiskRule` | `VENDOR_CONCENTRATION` | Employee has `>= 3` total transactions AND highest vendor share `>= 70.0%` of their transactions | `+15` | `MEDIUM` | `"Employee transactions are {X}% concentrated with vendor {vendor}"` |

### Scoring & Severity Aggregation Logic
1. **Additive Sum**: Total Risk Score = $\min\left(\sum \text{Finding Scores}, 100\right)$ (capped at 100).
2. **Severity Category Mapping**:
   - `CRITICAL`: Score $\ge 80$
   - `HIGH`: Score $60 \le \text{Score} < 80$
   - `MEDIUM`: Score $30 \le \text{Score} < 60$
   - `LOW`: Score $< 30$ ($0 \le \text{Score} \le 29$)

---

## 8. Automated Test Execution & Verified Results

Both the backend and frontend test suites were executed on the active system.

### 8.1 Backend Automated Tests (JUnit 5 + Testcontainers PostgreSQL 16)
- **Command:** `mvn clean test`
- **Output:**
  ```text
  [INFO] Results:
  [INFO] Tests run: 175, Failures: 0, Errors: 0, Skipped: 0
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] Total time: 04:39 min
  ```
- **Test Inventory Breakdown:**
  - `SecurityTests`: 41 tests (Password hashing, login verification, JWT token expiration, role-based authorization matrix, dev/prod admin bootstrap, admin user management, disabled-user immediate rejection, last-admin lockout).
  - `AuditControllerTest`: 6 tests (Principal actor enforcement, audit decision creation, event logging, transaction validation).
  - `RiskControllerIntegrationTest`: 38 tests (Single-transaction live analysis, persistence, history comparison, pagination, filtering by risk level and score).
  - `RiskEvidenceIntegrationTest`: 4 tests (Vendor, employee, and category relationship intelligence queries).
  - `TransactionImportControllerTest`: 5 tests (CSV file format validation, empty file rejection, successful upload).
  - `AuditServiceTest`: 14 tests (Business rule validation, analysisRunId validation, transaction rollback).
  - `RiskAnalysisServiceTest`: 3 tests (Summary generation, mock transaction batching).
  - `RiskEngineFactoryTest`: 3 tests (Rule wiring and configuration loading).
  - `RiskScoreCalculatorTest`: 10 tests (Capping at 100, threshold boundaries: 0, 29, 30, 59, 60, 79, 80, 100).
  - `RiskFindingRepositoryTest`: 2 tests (Batch finding queries, cascade deletion).
  - Deterministic Rule Unit Tests (`DuplicateTransactionRuleTest`, `HighAmountRuleTest`, `RoundAmountRuleTest`, `TransactionVelocityRuleTest`, `UnusualTimeRuleTest`, `VendorConcentrationRuleTest`): 38 tests.
  - `RiskEngineTest`: 2 tests (Combined engine analysis).
  - `TransactionImportServiceTest`: 1 test (Transactional multi-row CSV insert).

### 8.2 Frontend Automated Tests (Node.js Test Runner)
- **Command:** `cd frontend; npm test`
- **Output:**
  ```text
  ℹ tests 56
  ℹ suites 13
  ℹ pass 56
  ℹ fail 0
  ℹ cancelled 0
  ℹ skipped 0
  ℹ todo 0
  ℹ duration_ms 315.8637
  ```
- **Test Inventory Breakdown:**
  - `admin-user-management.test.mjs`: Role-based navigation visibility (ADMIN vs AUDITOR vs VIEWER), user creation form validation (length, required fields), self-management protection rule, server error message sanitization, disable confirmation state machine, client-side search/role/status filters, 5-second auto-dismiss alerts.
  - `session-expiry-logout.test.mjs`: Standard logout token clearing, HTTP 401 session expiration notification, deduplication of concurrent 401s, HTTP 403 Forbidden non-logout behavior, re-login recovery.

### 8.3 Frontend Production Build Validation
- **Command:** `cd frontend; npm run build`
- **Result:** TypeScript check (`tsc -b`) and Vite production bundle passed cleanly with **0 errors** in 14.87s.

---

## 9. Deployment Architecture

Orchestrated via `docker-compose.yml`:
- **`postgres` container (`postgres:16-alpine`)**:
  - Bound to internal `aurex-network` bridge network. Port 5432 is not exposed to the public host.
  - Volume `postgres_data` persists database files.
  - Initialized automatically on first start via `./src/main/resources/schema.sql` mounted into `/docker-entrypoint-initdb.d/schema.sql`.
  - Healthcheck: `pg_isready -U postgres -d financial_audit`.
- **`backend` container (Spring Boot 3.5.6 on Eclipse Temurin 17 JRE Alpine)**:
  - Multi-stage build (Maven 3.9 builder stage $\to$ lightweight JRE runtime).
  - Non-root runtime user (`aurexuser:aurexgroup`).
  - Connects to Postgres via internal DNS `jdbc:postgresql://postgres:5432/financial_audit`.
  - Production profile overrides: `spring.sql.init.mode=never`, Swagger UI disabled by default (`ENABLE_SWAGGER=false`), JWT secret minimum length check (32 chars) enforced at startup.
  - Healthcheck: `wget -q --spider http://127.0.0.1:8080/api/health`.
- **`frontend` container (React SPA on Nginx Alpine)**:
  - Multi-stage build (`node:20-alpine` build $\to$ `nginx:alpine` runtime).
  - **Publishes host port** `${FRONTEND_PORT:-80}` → internal container port `80`. This is the only container with an externally reachable port.
  - Reverse-proxies `/api/` requests to `http://backend:8080/api/` (internal Docker DNS) with streaming and 50MB client max body size.
  - Serves static assets with Gzip compression and long-term caching (`Cache-Control: public, immutable` — this is an HTTP caching directive for browser assets, not related to database-level immutability).
  - Implements SPA client routing fallback: all unmatched paths route to `/index.html`.

---

## 10. Known Limitations, Uncertainties & Verification Gaps

1. **Diagram Rendering Tooling**:
   - Local command-line rendering binaries (`mmdc` for Mermaid, `plantuml`, `graphviz dot`) are not installed in the Windows host PATH.
   - **Mitigation**: All diagram source files are generated in standard, syntax-validated Mermaid (`.mmd`) and PlantUML (`.puml`) formats in `docs/project-submission/diagrams/`, which render natively in GitHub, VS Code previewers, and online editors.
2. **Database Migration Tooling**:
   - The platform relies on idempotent DDL scripts (`schema.sql`) rather than Flyway or Liquibase versioned migration scripts. For production incremental schema updates, manual DDL or future migration tooling is required.
3. **No JPA/Hibernate**:
   - Confirmed: The repository explicitly avoids ORM frameworks. All queries are raw SQL executed via `JdbcTemplate` or `PreparedStatement`.
4. **Readme Product Roadmap vs Actual Code**:
   - The README product roadmap lists "AI-Assisted Investigation Assistant" and "Case Management Workflow". As verified, these are aspirational future enhancements and are not present in the codebase.
