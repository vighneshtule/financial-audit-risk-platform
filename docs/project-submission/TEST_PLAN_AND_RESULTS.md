# AUREX — Test Plan & Actual Verification Results

**Document Version:** 1.0  
**Project Name:** AUREX — Financial Audit Risk Platform  
**Academic Context:** BCA Field Project, MIT Arts, Commerce & Science College, Alandi  
**Test Frameworks:** JUnit 5 (5.12.2), Testcontainers PostgreSQL (1.21.4), Spring Boot Test (3.5.6), Node.js Test Runner  
**Test Run Execution Date:** October 2026  
**Test Environment:** Windows 11 (64-bit), OpenJDK 17.0.19, Node.js v24.11.1, Docker Desktop 29.1.3 (PostgreSQL 16)  

---

## 1. Test Strategy & Scope

The testing strategy for AUREX employs a multi-tiered verification approach:
1. **Unit Tests (Backend)**: Validates deterministic risk rule logic, scoring boundaries (capping at 100), severity tier thresholds, and CSV line parsing in total isolation without external dependencies.
2. **Integration Tests (Backend with Testcontainers)**: Boots the complete Spring Boot web context with MockMvc against an ephemeral, real PostgreSQL 16 container managed by Testcontainers. Tests full REST controller routing, Spring Security RBAC authorization filters, transactional JDBC commits, and database integrity constraints.
3. **Frontend Logic Tests (Node.js Test Runner)**: Tests client-side role guards, navigation visibility, form validation rules, confirmation state machines, client-side filters, and automatic session-expiry recovery.
4. **Planned Manual Test Cases**: Structured acceptance test scenarios for professor evaluation and viva demonstration.

---

## 2. Summary of Actual Automated Test Execution

| Test Suite / Category | Tool / Framework | Classes / Files | Tests Executed | Tests Passed | Failures | Errors | Skipped | Execution Time |
|---|---|---|---|---|---|---|---|---|
| **Backend Unit & Integration Tests** | Maven Surefire + JUnit 5 + Testcontainers | 18 Test Classes | **175** | **175** | **0** | **0** | **0** | **04:39 min** |
| **Frontend Automated Tests** | Node.js Test Runner (`node --test`) | 2 Test Files (13 Suites) | **56** | **56** | **0** | **0** | **0** | **315 ms** |
| **Frontend Production Build** | TypeScript Compiler (`tsc -b`) + Vite 8.3 | Entire SPA | **N/A** | **Passed** | **0** | **0** | **0** | **14.87 s** |
| **Total Automated Tests** | | | **231** | **231** | **0** | **0** | **0** | |

---

## 3. Backend Test Inventory & Requirement Traceability

### 3.1 Security & Authentication Tests (`SecurityTests.java`)
*Location: [`src/test/java/com/vighnesh/security/SecurityTests.java`](../../src/test/java/com/vighnesh/security/SecurityTests.java)*  
*Total Executed: 41 tests | Failures: 0 | Status: VERIFIED*

| Test ID | Mapped Requirement | Preconditions | Test Input / Action | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|
| `TC-SEC-01` | `FR-AUTH-01`, `NFR-SEC-01` | User `admin_user` created in database | Inspect `users.password_hash` | Password is not stored plaintext; starts with `$2a$` or `$2b$`; matches via `PasswordEncoder.matches`. | Verified hash prefix and BCrypt match | **PASS** |
| `TC-SEC-02` | `FR-AUTH-01`, `FR-AUTH-02` | Valid credentials exist | `POST /api/auth/login` with correct username & password | HTTP 200 OK; returns valid JWT token and `UserDto`; password fields omitted. | 200 OK, token issued, password absent | **PASS** |
| `TC-SEC-03` | `FR-AUTH-01` | Valid user exists | `POST /api/auth/login` with incorrect password | HTTP 401 Unauthorized with generic message `"Invalid username or password"`. | 401 Unauthorized | **PASS** |
| `TC-SEC-04` | `FR-AUTH-01` | Target user does not exist | `POST /api/auth/login` with unknown username | HTTP 401 Unauthorized with generic message `"Invalid username or password"`. | 401 Unauthorized | **PASS** |
| `TC-SEC-05` | `FR-AUTH-03` | User authenticated | `GET /api/auth/me` with header `Authorization: Bearer <valid_token>` | HTTP 200 OK with authenticated user profile. | 200 OK with username and role | **PASS** |
| `TC-SEC-06` | `FR-AUTH-03` | Request sent without token | `GET /api/auth/me` without Authorization header | HTTP 401 Unauthorized. | 401 Unauthorized | **PASS** |
| `TC-SEC-07` | `FR-AUTH-03` | Request sent with malformed token | `GET /api/auth/me` with header `Authorization: Bearer invalid_string` | HTTP 401 Unauthorized. | 401 Unauthorized | **PASS** |
| `TC-SEC-08` | `FR-AUTH-06`, `FR-USER-01` | Admin authenticated | `GET /api/users` with admin token | HTTP 200 OK returning array of users. | 200 OK | **PASS** |
| `TC-SEC-09` | `FR-AUTH-06`, `FR-USER-01` | Auditor authenticated | `GET /api/users` with auditor token | HTTP 403 Forbidden. | 403 Forbidden | **PASS** |
| `TC-SEC-10` | `FR-AUTH-06`, `FR-USER-01` | Viewer authenticated | `GET /api/users` with viewer token | HTTP 403 Forbidden. | 403 Forbidden | **PASS** |
| `TC-SEC-11` | `FR-AUTH-06`, `FR-INGEST-01`| Viewer authenticated | `POST /api/transactions/import` with viewer token | HTTP 403 Forbidden. | 403 Forbidden | **PASS** |
| `TC-SEC-12` | `FR-AUTH-06`, `FR-RISK-10` | Viewer authenticated | `POST /api/risk/analyze/TXN001` with viewer token | HTTP 403 Forbidden. | 403 Forbidden | **PASS** |
| `TC-SEC-13` | `FR-AUTH-06`, `FR-AUDIT-03`| Viewer authenticated | `POST /api/risk/.../audit/decisions` with viewer token | HTTP 403 Forbidden. | 403 Forbidden | **PASS** |
| `TC-SEC-14` | `FR-AUTH-06`, `FR-INGEST-04`| Viewer authenticated | `GET /api/transactions` and `GET /api/risk/summary` with viewer token | HTTP 200 OK (read-only allowed). | 200 OK | **PASS** |
| `TC-SEC-15` | `FR-AUTH-04` | Auditor authenticated with valid JWT | Admin disables auditor via `PATCH /api/users/{id}/status`; auditor sends `GET /api/auth/me` | HTTP 401 Unauthorized immediately, despite valid JWT signature. | 401 Unauthorized | **PASS** |
| `TC-SEC-16` | `FR-USER-04` | Admin authenticated | Admin sends `PATCH /api/users/{self_id}/status` with `enabled=false` | HTTP 409 Conflict (`"Cannot disable your own authenticated account"`). | 409 Conflict | **PASS** |
| `TC-SEC-17` | `FR-USER-05` | Single admin enabled in system | Request to disable the only active admin | HTTP 409 Conflict (`"Cannot disable the last enabled ADMIN account"`). | 409 Conflict | **PASS** |
| `TC-SEC-18` | `FR-METRIC-03` | No authentication | `GET /api/health` | HTTP 200 OK `{"status":"UP","database":"UP"}`. | 200 OK | **PASS** |

---

### 3.2 Audit Controller & Service Tests (`AuditControllerTest.java` & `AuditServiceTest.java`)
*Location: [`src/test/java/com/vighnesh/controller/AuditControllerTest.java`](../../src/test/java/com/vighnesh/controller/AuditControllerTest.java), [`src/test/java/com/vighnesh/service/AuditServiceTest.java`](../../src/test/java/com/vighnesh/service/AuditServiceTest.java)*  
*Total Executed: 20 tests (6 controller + 14 service) | Failures: 0 | Status: VERIFIED*

| Test ID | Mapped Requirement | Preconditions | Test Input / Action | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|
| `TC-AUD-01` | `FR-AUDIT-03`, `FR-AUDIT-04` | Transaction and run exist; auditor logged in | `POST .../audit/decisions` with `decidedBy = "fake_actor"` | Decision saved; `decided_by` in database equals authenticated principal name, ignoring client value. | `decided_by` matches Principal | **PASS** |
| `TC-AUD-02` | `FR-AUDIT-05` | Transaction exists | Submit valid decision | Both `audit_decisions` and `audit_events` records created with matching actor. | Dual records persisted | **PASS** |
| `TC-AUD-03` | `FR-AUDIT-03` | Target transaction ID does not exist | `POST /api/risk/transactions/UNKNOWN/audit/decisions` | HTTP 404 Not Found (`TransactionNotFoundException`). | 404 Not Found | **PASS** |
| `TC-AUD-04` | `FR-AUDIT-03` | No analysis run exists for transaction | Submit decision without analysisRunId | HTTP 404 Not Found (`AnalysisRunNotFoundException`). | 404 Not Found | **PASS** |
| `TC-AUD-05` | `FR-AUDIT-06` | Decisions exist for transaction | `GET .../audit/decisions` | HTTP 200 OK returning list sorted by `decided_at DESC`. | 200 OK array | **PASS** |
| `TC-AUD-06` | `FR-AUDIT-06` | Audit events exist | `GET .../audit/events` | HTTP 200 OK returning list of append-only event entries (no UPDATE/DELETE exposed). | 200 OK array | **PASS** |

---

### 3.3 Risk Controller & Evidence Integration Tests
*Location: `com.vighnesh.controller.RiskControllerIntegrationTest` & `RiskEvidenceIntegrationTest`*  
*Total Executed: 42 tests (38 controller + 4 evidence) | Failures: 0 | Status: VERIFIED*

| Test ID | Mapped Requirement | Preconditions | Test Input / Action | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|
| `TC-RSK-01` | `FR-RISK-09` | Ingested transactions exist | `GET /api/risk/transactions/{id}` | HTTP 200 OK `RiskReport` with computed score, severity, and findings list. | 200 OK, score matches policy sum | **PASS** |
| `TC-RSK-02` | `FR-RISK-10` | Ingested transactions exist | `POST /api/risk/analyze/{id}` | Run record written to `risk_analysis_runs`; findings written to `risk_findings`. | Database rows verified | **PASS** |
| `TC-RSK-03` | `FR-RISK-10` | Ingested transactions exist | `POST /api/risk/analyze-all` | Batch summary returned; all transactions evaluated and persisted atomically. | `AnalysisResult` returned | **PASS** |
| `TC-RSK-04` | `FR-RISK-11` | Multiple evaluated transactions | `GET /api/risk/transactions?riskLevel=HIGH&page=0&size=10` | Filtered paginated results containing only `HIGH` risk records. | Filter verified | **PASS** |
| `TC-RSK-05` | `FR-AUDIT-01` | Transactions sharing vendor/employee exist | `GET /api/risk/transactions/{id}/context` | Returns `TransactionContext` containing peer vendor, employee, and category groups. | Context groups populated | **PASS** |
| `TC-RSK-06` | `FR-AUDIT-02` | Multiple evaluation runs recorded | `GET /api/risk/transactions/{id}/history` | Returns chronological history of evaluation runs with findings comparison. | History array verified | **PASS** |

---

### 3.4 Deterministic Risk Rule Unit Tests
*Location: `src/test/java/rule/*` & `src/test/java/com/vighnesh/service/RiskScoreCalculatorTest.java`*  
*Total Executed: 48 tests | Failures: 0 | Status: VERIFIED*

| Test ID | Mapped Requirement | Test Target | Input Condition | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|
| `TC-RUL-01` | `FR-RISK-01` | `HighAmountRule` | Amount = 100,001 INR (> 100,000 threshold) | Returns `RiskFinding` with Score = 30, Severity = `MEDIUM`. | Finding triggered | **PASS** |
| `TC-RUL-02` | `FR-RISK-01` | `HighAmountRule` | Amount = 100,000 INR (equals threshold) | Returns `null` (not strictly greater). | No finding triggered | **PASS** |
| `TC-RUL-03` | `FR-RISK-02` | `UnusualTimeRule` | Time = 08:59:59 (before 09:00 start) | Returns `RiskFinding` with Score = 20, Severity = `MEDIUM`. | Finding triggered | **PASS** |
| `TC-RUL-04` | `FR-RISK-02` | `UnusualTimeRule` | Time = 14:30:00 (within business hours) | Returns `null`. | No finding triggered | **PASS** |
| `TC-RUL-05` | `FR-RISK-03` | `RoundAmountRule` | Amount = 50,000 INR (multiple of 10,000) | Returns `RiskFinding` with Score = 10, Severity = `MEDIUM`. | Finding triggered | **PASS** |
| `TC-RUL-06` | `FR-RISK-03` | `RoundAmountRule` | Amount = 50,001 INR | Returns `null`. | No finding triggered | **PASS** |
| `TC-RUL-07` | `FR-RISK-04` | `TransactionVelocityRule` | Employee has 3 transactions in 20 minutes | Returns `RiskFinding` with Score = 20, Severity = `MEDIUM`. | Finding triggered | **PASS** |
| `TC-RUL-08` | `FR-RISK-05` | `DuplicateTransactionRule` | Identical vendor, employee, amount, category in 5 mins | Returns `RiskFinding` with Score = 25, Severity = `MEDIUM`. | Finding triggered | **PASS** |
| `TC-RUL-09` | `FR-RISK-06` | `VendorConcentrationRule` | Employee has 4 txns, 3 with Vendor A (75% > 70%) | Returns `RiskFinding` with Score = 15, Severity = `MEDIUM`. | Finding triggered | **PASS** |
| `TC-RUL-10` | `FR-RISK-07` | `RiskScoreCalculator` | Findings summing to 120 points | Returns score capped at 100. | Total score = 100 | **PASS** |
| `TC-RUL-11` | `FR-RISK-08` | `RiskScoreCalculator` | Scores = 29, 30, 59, 60, 79, 80 | Maps to `LOW`, `MEDIUM`, `MEDIUM`, `HIGH`, `HIGH`, `CRITICAL` respectively. | Exact boundaries verified | **PASS** |

---

### 3.5 Transaction CSV Import Tests (`TransactionImportControllerTest.java` & `TransactionImportServiceTest.java`)
*Location: `src/test/java/com/vighnesh/controller/TransactionImportControllerTest.java` & `service.TransactionImportServiceTest`*  
*Total Executed: 6 tests | Failures: 0 | Status: VERIFIED*

| Test ID | Mapped Requirement | Preconditions | Test Input / Action | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|
| `TC-IMP-01` | `FR-INGEST-01`, `FR-INGEST-03`| Valid CSV file | Upload 5-row standard CSV | HTTP 200 OK; `importedCount = 5`; rows visible in database. | 200 OK, 5 rows inserted | **PASS** |
| `TC-IMP-02` | `FR-INGEST-02` | Empty file | Upload 0-byte file | HTTP 400 Bad Request (`"Uploaded file is missing or empty"`). | 400 Bad Request | **PASS** |
| `TC-IMP-03` | `FR-INGEST-02` | Non-CSV extension | Upload `data.xlsx` or `transactions.txt` | HTTP 400 Bad Request (`"Only CSV files are accepted"`). | 400 Bad Request | **PASS** |

---

## 4. Frontend Automated Test Inventory (`node --test`)
*Location: [`frontend/src/__tests__/admin-user-management.test.mjs`](../../frontend/src/__tests__/admin-user-management.test.mjs), [`frontend/src/__tests__/session-expiry-logout.test.mjs`](../../frontend/src/__tests__/session-expiry-logout.test.mjs)*  
*Total Executed: 56 tests across 13 suites | Failures: 0 | Status: VERIFIED*

- **Suite 1: Role-based Navigation Visibility (3 tests)**:
  - `ADMIN` role includes Users link in sidebar navigation.
  - `AUDITOR` role excludes Users link.
  - `VIEWER` role excludes Users link.
- **Suite 2: User Creation Validation (5 tests)**:
  - Rejects empty or whitespace username.
  - Rejects empty password.
  - Rejects password shorter than 6 characters.
  - Accepts valid `AUDITOR` and `VIEWER` payloads.
- **Suite 3: Self-Management Protection Rule (2 tests)**:
  - Disallows disabling currently authenticated account ID.
  - Allows disabling a different user account.
- **Suite 4: Server Error Safe Message Handling (4 tests)**:
  - Safely parses and surfaces 409 duplicate username error.
  - Safely parses and surfaces 409 last-admin lockout error.
  - Safely handles 403 access denied and 404 user not found errors.
- **Suite 5: Disable Confirmation State Machine (7 tests)**:
  - Clicking "Disable" enters `confirmingId` state without calling API.
  - "Cancel" resets state without calling API.
  - Only one row can be in confirmation state simultaneously.
  - Modal dismisses on Escape key or backdrop click.
- **Suite 6: Enable Path Immediate Execution (2 tests)**:
  - Enabling an account calls API immediately without confirmation step.
- **Suite 7: Self-Disable Protection (5 tests)**:
  - Validates case-insensitive username matching and ID matching for self-disable blocking.
- **Suite 8: Client-Side Search, Role, and Status Filters (14 tests)**:
  - Full permutation tests for username substring search, role filter, status filter, combined filters, and filter clearing.
- **Suite 9: Success-Message Auto-Dismiss (3 tests)**:
  - Verifies 5-second timer auto-dismisses alerts; timer cancellation on re-entry.
- **Suite 10: Logout & Session Expiry UX (6 tests)**:
  - Normal logout clears token and storage without error banner.
  - HTTP 401 interceptor sets `sessionExpired = true` and clears auth.
  - Concurrent duplicate 401s trigger only one banner notification.
  - HTTP 403 Forbidden does not log user out or clear JWT.
  - Re-login resets sessionExpired state cleanly.

---

## 5. Planned Manual Test Cases for Viva Demonstration

The following manual test cases are prepared for live academic demonstration before examination evaluators:

| Manual Test ID | Scenario | Steps to Demonstrate | Expected Visual & System Result |
|---|---|---|---|
| **MAN-01** | **Role-Based UI Partitioning** | 1. Log in as `admin`. Observe all tabs visible (`Dashboard`, `Transactions`, `Intelligence`, `Investigations`, `History`, `Import`, `Users`).<br/>2. Log out and log in as an `auditor`.<br/>3. Observe that the `Users` tab is absent from navigation.<br/>4. Log out and log in as a `viewer`. Observe that both `Import` and `Users` tabs are absent. | UI dynamically adapts to active JWT role; unpermitted navigation items are completely hidden. |
| **MAN-02** | **Transaction CSV Upload & Atomic Commit** | 1. In `auditor` session, navigate to `/import`.<br/>2. Select sample file `transactions_sample.csv` (10 rows).<br/>3. Click "Upload & Ingest".<br/>4. Navigate to `/transactions`. | Confirmation banner reports "10 transactions imported successfully"; rows render immediately in transaction list. |
| **MAN-03** | **Live Forensic Investigation Workspace** | 1. Open transaction `TXN-90214` (amount 150,000 INR, 21:45 PM).<br/>2. View the animated Risk Score Gauge (60/100, `HIGH`).<br/>3. Review the findings cards (High Amount +30, Unusual Time +20, Round Amount +10).<br/>4. Open the Evidence Context tab to view peer vendor transactions. | Clear, visual breakdown of mathematical score composition and cross-entity evidence graph. |
| **MAN-04** | **Submitting Audit Determination** | 1. In the investigation workspace for `TXN-90214`, click "Record Decision".<br/>2. Select `REQUIRES_INVESTIGATION`.<br/>3. Enter comment: `"Off-hours round transfer requires vendor contract validation"`.<br/>4. Click Submit. | Modal closes; decision badge updates; audit history displays recorded actor matching the logged-in auditor. |
| **MAN-05** | **Last-Admin Lockout Prevention** | 1. Log in as `admin`. Navigate to `/users`.<br/>2. Attempt to disable the only active `admin` account.<br/>3. Click Confirm Disable. | UI displays error notification: `"Cannot disable the last enabled ADMIN account"`; account remains enabled. |
| **MAN-06** | **Immediate Session Revocation on Account Disablement** | 1. Open two browser windows (Window A as `admin`, Window B as `auditor`).<br/>2. In Window A, disable the auditor account.<br/>3. In Window B, click any navigation tab or trigger an API request. | Window B is immediately kicked to `/login` with the alert: `"Your session has expired. Please log in again."`. |
