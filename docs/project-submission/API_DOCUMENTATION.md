# AUREX — REST API Specification & Endpoint Inventory

**Document Version:** 1.0  
**Project Name:** AUREX — Financial Audit Risk Platform  
**Academic Context:** BCA Field Project, MIT Arts, Commerce & Science College, Alandi  
**API Documentation Format:** OpenAPI 3.0 / Springdoc Swagger UI  
**Swagger UI Path:** `http://localhost:8080/swagger-ui.html` (or `http://localhost/swagger-ui.html` when proxied)  

---

## 1. Authentication & Common Error Format

### 1.1 Authentication Header
All protected endpoints require a valid JSON Web Token (JWT) passed via standard HTTP header:
```http
Authorization: Bearer <eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...>
```

### 1.2 Centralized JSON Error Format
All exceptions intercepted by `GlobalExceptionHandler.java` produce a standard error response structure:
```json
{
  "timestamp": "2026-10-15T09:30:00.123456",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error cause description",
  "path": "/api/transactions/import"
}
```

Common status codes:
- `400 Bad Request`: Validation failure, ill-formed JSON, missing parameter, or constraint violation.
- `401 Unauthorized`: Missing, expired, or invalid JWT; invalid login credentials; or account disabled.
- `403 Forbidden`: Authenticated identity lacks the required role authority for the endpoint.
- `404 Not Found`: Requested transaction, analysis run, or user ID does not exist.
- `409 Conflict`: Business rule conflict (e.g., duplicate username, self-disable, last-admin lockout).
- `500 Internal Server Error`: Unhandled server exception.

---

## 2. Complete Endpoint Inventory

### 2.1 Authentication Controller (`AuthController.java`)

#### 1. `POST /api/auth/login`
- **Purpose:** Authenticates user credentials and issues a signed JWT.
- **Authentication:** None (Public / `permitAll()`).
- **Required Role:** None.
- **Request Headers:** `Content-Type: application/json`
- **Request Body (`LoginRequest`):**
  ```json
  {
    "username": "admin_user",
    "password": "AdminPassword123!"
  }
  ```
- **Validation Rules:** `username` must not be blank; `password` must not be blank.
- **Response Codes:**
  - `200 OK`: Successful authentication.
  - `400 Bad Request`: Missing username or password.
  - `401 Unauthorized`: Invalid username, incorrect password, or disabled account.
- **Response Body (`LoginResponse`):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1Ni...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "username": "admin_user",
      "role": "ADMIN",
      "enabled": true,
      "createdAt": "2026-10-15T08:00:00",
      "updatedAt": "2026-10-15T08:00:00"
    }
  }
  ```

#### 2. `GET /api/auth/me`
- **Purpose:** Retrieves identity and profile information for the currently authenticated caller.
- **Authentication:** Required (`Authorization: Bearer <token>`).
- **Required Role:** Any Authenticated (`ADMIN`, `AUDITOR`, `VIEWER`).
- **Response Codes:**
  - `200 OK`: Returns authenticated user details.
  - `401 Unauthorized`: Missing or invalid token.
- **Response Body (`UserDto`):**
  ```json
  {
    "id": 2,
    "username": "auditor_user",
    "role": "AUDITOR",
    "enabled": true,
    "createdAt": "2026-10-15T08:15:00",
    "updatedAt": "2026-10-15T08:15:00"
  }
  ```

---

### 2.2 User Management Controller (`UserController.java`)

#### 3. `GET /api/users`
- **Purpose:** Fetches complete list of all registered platform users.
- **Authentication:** Required.
- **Required Role:** `ADMIN` (enforced via `.hasRole("ADMIN")` and `@PreAuthorize("hasRole('ADMIN')")`).
- **Response Codes:**
  - `200 OK`: User list returned.
  - `401 Unauthorized`: Unauthenticated.
  - `403 Forbidden`: Non-admin caller.
- **Response Body (`List<UserDto>`):**
  ```json
  [
    {
      "id": 1,
      "username": "admin",
      "role": "ADMIN",
      "enabled": true,
      "createdAt": "2026-10-15T08:00:00",
      "updatedAt": "2026-10-15T08:00:00"
    },
    {
      "id": 2,
      "username": "auditor_vighnesh",
      "role": "AUDITOR",
      "enabled": true,
      "createdAt": "2026-10-15T08:15:00",
      "updatedAt": "2026-10-15T08:15:00"
    }
  ]
  ```

#### 4. `POST /api/users`
- **Purpose:** Provisions a new user account.
- **Authentication:** Required.
- **Required Role:** `ADMIN`.
- **Request Body (`CreateUserRequest`):**
  ```json
  {
    "username": "new_auditor",
    "password": "SecurePassword123!",
    "role": "AUDITOR"
  }
  ```
- **Validation Rules:**
  - `username`: Required, non-blank.
  - `password`: Required, non-blank, minimum 6 characters.
  - `role`: Required, valid `UserRole` enum (`ADMIN`, `AUDITOR`, `VIEWER`).
- **Response Codes:**
  - `201 Created`: User created successfully.
  - `400 Bad Request`: Validation failure.
  - `401 Unauthorized`: Unauthenticated.
  - `403 Forbidden`: Non-admin caller.
  - `409 Conflict`: Username already exists.
- **Response Body (`UserDto`):** Returns created user object (passwords are omitted).

#### 5. `PATCH /api/users/{id}/status`
- **Purpose:** Enables or disables a user account.
- **Authentication:** Required.
- **Required Role:** `ADMIN`.
- **Path Parameter:** `id` (Long, user primary key).
- **Request Body (`UpdateUserStatusRequest`):**
  ```json
  {
    "enabled": false
  }
  ```
- **Validation & Safety Rules:**
  - `enabled`: Required boolean.
  - **Self-Management Check:** If admin targets their own ID or username, returns HTTP 409 Conflict (`"Cannot disable your own authenticated account"`).
  - **Last-Admin Lockout Check:** If target is `ADMIN` and active admin count $\le 1$, returns HTTP 409 Conflict (`"Cannot disable the last enabled ADMIN account"`).
- **Response Codes:**
  - `200 OK`: Status updated.
  - `400 Bad Request`: Missing `enabled` property.
  - `401 Unauthorized`: Unauthenticated.
  - `403 Forbidden`: Non-admin caller.
  - `404 Not Found`: Target user ID does not exist.
  - `409 Conflict`: Self-disable or last-admin lockout violation.
- **Response Body (`UserDto`):** Updated user profile.

---

### 2.3 Transaction Ingestion Controller (`TransactionImportController.java` & `TransactionController.java`)

#### 6. `POST /api/transactions/import`
- **Purpose:** Uploads and ingests a batch financial transactions CSV file atomically.
- **Authentication:** Required.
- **Required Role:** `ADMIN` or `AUDITOR` (`.hasAnyRole("ADMIN", "AUDITOR")`).
- **Request Content-Type:** `multipart/form-data`
- **Request Parameter:** `file` (MultipartFile, required, must end with `.csv`).
- **Validation Rules:** File must not be empty; filename must be present and end with `.csv`; file size must not exceed 50MB.
- **Response Codes:**
  - `200 OK`: File parsed and batch inserted successfully.
  - `400 Bad Request`: Missing file, empty file, non-CSV format, or SQL batch insertion failure.
  - `401 Unauthorized`: Unauthenticated.
  - `403 Forbidden`: Role `VIEWER` attempted import.
- **Response Body (`ImportResponse`):**
  ```json
  {
    "message": "Transactions imported successfully",
    "importedCount": 150
  }
  ```

#### 7. `GET /api/transactions`
- **Purpose:** Fetches all ingested transactions ordered chronologically.
- **Authentication:** Required.
- **Required Role:** Any Authenticated (`ADMIN`, `AUDITOR`, `VIEWER`).
- **Response Codes:**
  - `200 OK`: List of transactions.
  - `401 Unauthorized`: Unauthenticated.
- **Response Body (`List<Transaction>`):**
  ```json
  [
    {
      "id": "TXN-001",
      "vendor": "Acme Supplies Ltd",
      "employee": "EMP-101",
      "amount": 125000.00,
      "transactionTime": "2026-10-14T19:30:00",
      "category": "HARDWARE"
    }
  ]
  ```

---

### 2.4 Risk Analysis Controller (`RiskController.java`)

#### 8. `GET /api/risk/summary`
- **Purpose:** Returns aggregate executive risk counts and metrics across all transactions.
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Response Body (`RiskSummary`):**
  ```json
  {
    "totalTransactions": 150,
    "totalAmount": 14250000.00,
    "lowRiskCount": 95,
    "mediumRiskCount": 35,
    "highRiskCount": 15,
    "criticalRiskCount": 5,
    "totalFindingsCount": 110,
    "highestRiskTransactionId": "TXN-042",
    "highestRiskScore": 85
  }
  ```

#### 9. `GET /api/risk/transactions`
- **Purpose:** Fetches a paginated list of evaluated transactions with optional risk level and score filters.
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Query Parameters:**
  - `riskLevel` (RiskSeverity, optional): Filter by `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
  - `minScore` (Integer, optional): Filter transactions with `riskScore >= minScore`.
  - `page` (int, optional, default: 0): Zero-indexed page index (must be $\ge 0$).
  - `size` (int, optional, default: 10): Items per page (must be $> 0$).
- **Response Body (`RiskTransactionPage`):**
  ```json
  {
    "content": [
      {
        "id": "TXN-001",
        "vendor": "Acme Supplies Ltd",
        "employee": "EMP-101",
        "amount": 125000.00,
        "category": "HARDWARE",
        "riskScore": 60,
        "riskLevel": "HIGH",
        "findings": [
          {
            "type": "HIGH_AMOUNT",
            "score": 30,
            "severity": "MEDIUM",
            "explanation": "Transaction amount exceeds configured threshold of INR 100000"
          }
        ]
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 150,
    "totalPages": 15
  }
  ```

#### 10. `GET /api/risk/transactions/{transactionId}`
- **Purpose:** Runs real-time in-memory risk evaluation for a single transaction and returns the report without writing to the database.
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Path Parameter:** `transactionId` (String).
- **Response Codes:**
  - `200 OK`: Evaluation completed.
  - `404 Not Found`: Transaction ID does not exist.
- **Response Body (`RiskReport`):**
  ```json
  {
    "riskScore": 60,
    "riskLevel": "HIGH",
    "findings": [
      {
        "type": "HIGH_AMOUNT",
        "score": 30,
        "severity": "MEDIUM",
        "explanation": "Transaction amount exceeds configured threshold of INR 100000"
      },
      {
        "type": "UNUSUAL_TRANSACTION_TIME",
        "score": 20,
        "severity": "MEDIUM",
        "explanation": "Transaction occurred outside normal business hours"
      },
      {
        "type": "ROUND_AMOUNT",
        "score": 10,
        "severity": "MEDIUM",
        "explanation": "Transaction amount is a round multiple of INR 10000"
      }
    ]
  }
  ```

#### 11. `POST /api/risk/analyze/{transactionId}`
- **Purpose:** Evaluates risk policies for a specific transaction and persists the evaluation run and individual findings into the database.
- **Authentication:** Required.
- **Required Role:** `ADMIN` or `AUDITOR`.
- **Response Codes:**
  - `200 OK`: Evaluated and persisted.
  - `403 Forbidden`: Caller lacks `ROLE_ADMIN` or `ROLE_AUDITOR`.
  - `404 Not Found`: Transaction does not exist.
- **Response Body (`RiskReport`):** Risk report detailing saved score, level, and findings.

#### 12. `POST /api/risk/analyze-all`
- **Purpose:** Evaluates every transaction in the database in a single atomic batch, persisting run records into `risk_analysis_runs` and findings into `risk_findings`.
- **Authentication:** Required.
- **Required Role:** `ADMIN` or `AUDITOR`.
- **Response Codes:**
  - `200 OK`: Batch evaluation persisted.
  - `403 Forbidden`: Caller lacks permission.
- **Response Body (`AnalysisResult`):**
  ```json
  {
    "totalTransactions": 150,
    "lowRiskCount": 95,
    "mediumRiskCount": 35,
    "highRiskCount": 15,
    "criticalRiskCount": 5,
    "highestRiskScore": 85
  }
  ```

#### 13. `GET /api/risk/transactions/{transactionId}/context`
- **Purpose:** Returns cross-entity relationship context graphs (peer transactions sharing the same vendor, employee, and category).
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Response Body (`TransactionContext`):**
  ```json
  {
    "transactionId": "TXN-001",
    "vendorGroup": {
      "groupKey": "Acme Supplies Ltd",
      "count": 12,
      "totalAmount": 1450000.00,
      "peakScore": 60,
      "transactions": [...]
    },
    "employeeGroup": {
      "groupKey": "EMP-101",
      "count": 5,
      "totalAmount": 420000.00,
      "peakScore": 60,
      "transactions": [...]
    },
    "categoryGroup": {
      "groupKey": "HARDWARE",
      "count": 28,
      "totalAmount": 3800000.00,
      "peakScore": 75,
      "transactions": [...]
    }
  }
  ```

#### 14. `GET /api/risk/transactions/{transactionId}/history`
- **Purpose:** Retrieves all historical risk analysis runs for a transaction.
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Response Body (`RiskAnalysisHistoryResponse`):**
  ```json
  {
    "transactionId": "TXN-001",
    "history": [
      {
        "analysisRunId": 1042,
        "analyzedAt": "2026-10-15T08:30:15",
        "riskScore": 60,
        "riskLevel": "HIGH",
        "findings": [...]
      }
    ]
  }
  ```

---

### 2.5 Audit Decision Controller (`AuditController.java`)

#### 15. `POST /api/risk/transactions/{transactionId}/audit/decisions`
- **Purpose:** Records a formal audit decision and creates an append-only audit event entry (application-level; the table exposes no UPDATE/DELETE API).
- **Authentication:** Required.
- **Required Role:** `ADMIN` or `AUDITOR`.
- **Request Body (`CreateAuditDecisionRequest`):**
  ```json
  {
    "analysisRunId": 1042,
    "decision": "REQUIRES_INVESTIGATION",
    "comment": "Off-hours round transfer requires vendor contract validation",
    "decidedBy": "ignored_client_value"
  }
  ```
- **Validation & Security Behavior:**
  - `decision`: Required, must be one of `CONFIRMED_RISK`, `FALSE_POSITIVE`, `REQUIRES_INVESTIGATION`, `ESCALATED`.
  - `comment`: Optional, maximum 2,000 characters.
  - `decidedBy`: **Ignored by backend**. The actor is derived exclusively from the authenticated Spring Security `Principal`.
- **Response Codes:**
  - `201 Created`: Decision and audit event created.
  - `400 Bad Request`: Validation failure.
  - `401 Unauthorized`: Bad credentials or missing principal.
  - `403 Forbidden`: Non-auditor / non-admin caller.
  - `404 Not Found`: Transaction or specified analysis run ID does not exist.
- **Response Body (`AuditDecision`):**
  ```json
  {
    "id": 501,
    "transactionId": "TXN-001",
    "analysisRunId": 1042,
    "decision": "REQUIRES_INVESTIGATION",
    "comment": "Off-hours round transfer requires vendor contract validation",
    "decidedBy": "auditor_vighnesh",
    "decidedAt": "2026-10-15T09:15:00"
  }
  ```

#### 16. `GET /api/risk/transactions/{transactionId}/audit/decisions`
- **Purpose:** Fetches complete historical list of audit decisions recorded for this transaction.
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Response Body (`List<AuditDecision>`):** Array of decisions ordered by `decided_at DESC`.

#### 17. `GET /api/risk/transactions/{transactionId}/audit/decision`
- **Purpose:** Fetches the most recent audit decision for the transaction.
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Response Body (`AuditDecision`):** Latest decision object (or null/empty if none recorded).

#### 18. `GET /api/risk/transactions/{transactionId}/audit/events`
- **Purpose:** Fetches append-only chronological audit events associated with the transaction (application-level; events cannot be modified or deleted through any API).
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Response Body (`List<AuditEvent>`):**
  ```json
  [
    {
      "id": 901,
      "transactionId": "TXN-001",
      "decisionId": 501,
      "eventType": "AUDIT_DECISION_CREATED",
      "eventDetails": "Audit decision REQUIRES_INVESTIGATION recorded for analysis run 1042",
      "actor": "auditor_vighnesh",
      "createdAt": "2026-10-15T09:15:00"
    }
  ]
  ```

---

### 2.6 Risk Intelligence Controller (`RiskIntelligenceController.java`)

#### 19. `GET /api/risk/intelligence/summary`
- **Purpose:** Fetches multidimensional analytical metrics across dataset overview, severity counts, vendor risk, employee risk, category risk, and risk type distributions.
- **Authentication:** Required.
- **Required Role:** Any Authenticated.
- **Response Body (`RiskIntelligenceSummary`):**
  ```json
  {
    "overview": {
      "totalTransactions": 150,
      "analyzedTransactions": 150,
      "totalAuditValue": 14250000.00,
      "totalFindings": 110,
      "lowRiskCount": 95,
      "mediumRiskCount": 35,
      "highRiskCount": 15,
      "criticalRiskCount": 5,
      "highestRiskScore": 85
    },
    "severityDistribution": [
      {"severity": "LOW", "count": 95},
      {"severity": "MEDIUM", "count": 35},
      {"severity": "HIGH", "count": 15},
      {"severity": "CRITICAL", "count": 5}
    ],
    "vendorRisk": [
      {
        "vendor": "Apex Logistics Pvt Ltd",
        "transactionCount": 18,
        "flaggedTransactionCount": 6,
        "totalAmount": 2100000.00,
        "highestRiskScore": 75,
        "findingCount": 12
      }
    ],
    "employeeRisk": [
      {
        "employee": "EMP-401",
        "transactionCount": 12,
        "flaggedTransactionCount": 5,
        "totalAmount": 1600000.00,
        "highestRiskScore": 60,
        "findingCount": 9
      }
    ],
    "categoryRisk": [
      {
        "category": "LOGISTICS",
        "transactionCount": 24,
        "flaggedTransactionCount": 8,
        "totalAmount": 2800000.00,
        "findingCount": 15
      }
    ],
    "riskTypeDistribution": [
      {
        "riskType": "HIGH_AMOUNT",
        "findingCount": 42,
        "totalScoreContribution": 1260
      },
      {
        "riskType": "UNUSUAL_TRANSACTION_TIME",
        "findingCount": 31,
        "totalScoreContribution": 620
      }
    ]
  }
  ```

---

### 2.7 Diagnostic Health Controller (`HealthController.java`)

#### 20. `GET /api/health`
- **Purpose:** Public readiness probe verifying application server and PostgreSQL database connectivity.
- **Authentication:** None (Public / `permitAll()`).
- **Response Codes:**
  - `200 OK`: Application and database are healthy.
  - `503 Service Unavailable`: Database connection is closed or unreachable.
- **Response Body:**
  ```json
  {
    "status": "UP",
    "database": "UP"
  }
  ```
