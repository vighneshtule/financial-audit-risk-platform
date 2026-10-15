# AUREX — Requirements Traceability Matrix (RTM)

| Document Metadata | Details |
| :--- | :--- |
| **Project** | AUREX — Financial Audit Risk Platform |
| **Document Classification** | Requirements Traceability Matrix (RTM) |
| **Academic Context** | BCA Field Project, MIT Arts, Commerce & Science College, Alandi |
| **Status** | Production / Project Submission Verified |
| **Traceability Status Categories** | `VERIFIED` (Automated & Manual tests pass), `PARTIALLY VERIFIED` (Verified with operational limits), `PLANNED` (Roadmap only) |

---

## 1. Overview and Traceability Methodology

The Requirements Traceability Matrix (RTM) establishes bidirectional traceability across all phases of the AUREX software engineering lifecycle:
1. **Business & System Requirements**: Defined in [PRD.md](PRD.md) and [SYSTEM_REQUIREMENTS.md](SYSTEM_REQUIREMENTS.md).
2. **Architecture & Design**: Detailed in [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md), [CLASS_DIAGRAM.md](CLASS_DIAGRAM.md), [ER_DIAGRAM.md](ER_DIAGRAM.md), and [API_DOCUMENTATION.md](API_DOCUMENTATION.md).
3. **Source Code Implementation**: Real Java classes in `com.vighnesh.*` and React components in `frontend/src/*`.
4. **Verification & Testing**: Automated JUnit 5 / Testcontainers backend tests (175 tests) and Node.js frontend tests (56 tests) documented in [TEST_PLAN_AND_RESULTS.md](TEST_PLAN_AND_RESULTS.md).

---

## 2. Functional Requirements Traceability Matrix

### 2.1 Authentication & Authorization Module (`FR-AUTH`)

| Req ID | Requirement Description | Implemented Classes / Methods / Endpoints | Test Case ID | Test Artifact / Execution Evidence | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **FR-AUTH-01** | Secure user login via username/password returning signed HMAC-SHA256 JWT | `AuthController.login()`, `AuthService.authenticate()`, `JwtTokenProvider.generateToken()`, `POST /api/auth/login` | `TC-AUTH-01` | `AuthServiceTest.java`, `AuthControllerTest.java` (PASS) | `VERIFIED` |
| **FR-AUTH-02** | Passwords salted and hashed with BCrypt (10 rounds) before storage | `BCryptPasswordEncoder`, `JdbcUserRepository.createUser()` | `TC-SEC-01` | `UserRepositoryTest.java` verifying hashed storage (PASS) | `VERIFIED` |
| **FR-AUTH-03** | Stateless JWT extraction, signature verification, and expiration enforcement | `JwtAuthenticationFilter.doFilterInternal()`, `JwtTokenProvider.validateToken()` | `TC-AUTH-02` | `JwtAuthenticationFilterTest.java` (PASS) | `VERIFIED` |
| **FR-AUTH-04** | Real-time database check on each request; reject disabled accounts immediately | `JwtAuthenticationFilter.doFilterInternal()`, `UserRepository.findByUsername()` | `TC-SEC-03` | `JwtAuthenticationFilterTest.java` verifying 401 on disabled user (PASS) | `VERIFIED` |
| **FR-AUTH-05** | Role-Based Access Control (`ADMIN`, `AUDITOR`, `VIEWER`) across all API endpoints | `SecurityConfig.securityFilterChain()`, `hasRole(...)` matching | `TC-SEC-02` | `SecurityAccessTest.java`, `UserManagementSecurityTest.java` (PASS) | `VERIFIED` |
| **FR-AUTH-06** | Retrieval of authenticated user session details via `/api/auth/me` | `AuthController.getCurrentUser()`, `SecurityContextHolder`, `GET /api/auth/me` | `TC-AUTH-03` | `AuthControllerTest.java` (PASS) | `VERIFIED` |

---

### 2.2 User Administration Module (`FR-USER`)

| Req ID | Requirement Description | Implemented Classes / Methods / Endpoints | Test Case ID | Test Artifact / Execution Evidence | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **FR-USER-01** | User account provisioning with username, role, and temporary password | `UserAdminController.createUser()`, `UserService.createUser()`, `POST /api/admin/users` | `TC-USR-01` | `UserAdminControllerTest.java`, `UserServiceTest.java` (PASS) | `VERIFIED` |
| **FR-USER-02** | User account listing with role and active status flags | `UserAdminController.listUsers()`, `GET /api/admin/users` | `TC-USR-02` | `UserAdminControllerTest.java` (PASS) | `VERIFIED` |
| **FR-USER-03** | User account deactivation and reactivation | `UserAdminController.setUserStatus()`, `PATCH /api/admin/users/{id}/status` | `TC-USR-03` | `UserServiceTest.java`, `UserAdminControllerTest.java` (PASS) | `VERIFIED` |
| **FR-USER-04** | Role modification for existing users | `UserAdminController.updateUserRole()`, `PATCH /api/admin/users/{id}/role` | `TC-USR-04` | `UserServiceTest.java` (PASS) | `VERIFIED` |
| **FR-USER-05** | Self-lockout prevention preventing admin from disabling own account | `UserService.setUserStatus()`, check `currentUser.getId().equals(targetId)` | `TC-USR-05` | `UserServiceTest.java` verifying `BadRequestException` (PASS) | `VERIFIED` |

---

### 2.3 Transaction Ingestion & Management (`FR-INGEST`)

| Req ID | Requirement Description | Implemented Classes / Methods / Endpoints | Test Case ID | Test Artifact / Execution Evidence | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **FR-INGEST-01** | RFC 4180 CSV batch parsing with format validation and error handling | `CsvTransactionParser.parse()`, `TransactionService.importCsv()`, `POST /api/transactions/import` | `TC-CSV-01` | `CsvTransactionParserTest.java` (PASS) | `VERIFIED` |
| **FR-INGEST-02** | Atomic batch insertion using plain JDBC `batchUpdate` | `JdbcTransactionRepository.saveAll()`, `JdbcTemplate.batchUpdate()` | `TC-CSV-02` | `TransactionRepositoryTest.java` (PASS) | `VERIFIED` |
| **FR-INGEST-03** | Single transaction manual creation with validation | `TransactionController.createTransaction()`, `POST /api/transactions` | `TC-TXN-01` | `TransactionControllerTest.java` (PASS) | `VERIFIED` |
| **FR-INGEST-04** | Paginated transaction listing with multi-field search and filtering | `TransactionController.getTransactions()`, `GET /api/transactions` | `TC-TXN-02` | `TransactionControllerTest.java`, `TransactionRepositoryTest.java` (PASS) | `VERIFIED` |

---

### 2.4 Deterministic Risk Rules & Analysis Engine (`FR-RISK`)

| Req ID | Requirement Description | Implemented Classes / Methods / Endpoints | Test Case ID | Test Artifact / Execution Evidence | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **FR-RISK-01** | In-memory deterministic rule execution with pluggable `RiskRule` interface | `RiskEngineService.analyzeTransaction()`, `List<RiskRule>` bean autowiring | `TC-RSK-01` | `RiskEngineServiceTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-02** | `HighAmountRule`: Trigger if `amount > 100,000.00` (+30 pts, `MEDIUM`) | `HighAmountRule.evaluate()`, `THRESHOLD = 100000.0` | `TC-RSK-02` | `HighAmountRuleTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-03** | `UnusualTimeRule`: Trigger if hour < 09:00 or >= 18:00 (+20 pts, `MEDIUM`) | `UnusualTimeRule.evaluate()`, `START=9, END=18` | `TC-RSK-03` | `UnusualTimeRuleTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-04** | `RoundAmountRule`: Trigger if amount > 0 and `amount % 10,000 == 0` (+10 pts, `MEDIUM`) | `RoundAmountRule.evaluate()`, modulo check | `TC-RSK-04` | `RoundAmountRuleTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-05** | `TransactionVelocityRule`: Trigger if employee has $\ge 3$ txns in 30-min window (+20 pts, `MEDIUM`) | `TransactionVelocityRule.evaluate()`, `JdbcTransactionRepository.findRecentTransactions()` | `TC-RSK-05` | `TransactionVelocityRuleTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-06** | `DuplicateTransactionRule`: Trigger if identical vendor, employee, amount, category within 10 min (+25 pts, `MEDIUM`) | `DuplicateTransactionRule.evaluate()`, `JdbcTransactionRepository.findDuplicates()` | `TC-RSK-06` | `DuplicateTransactionRuleTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-07** | `VendorConcentrationRule`: Trigger if employee has $\ge 3$ txns and single vendor share $\ge 70\%$ (+15 pts, `MEDIUM`) | `VendorConcentrationRule.evaluate()`, `JdbcTransactionRepository.findEmployeeVendorStats()` | `TC-RSK-07` | `VendorConcentrationRuleTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-08** | Score aggregation: sum of finding scores, capped at maximum 100 | `RiskEngineService.analyzeTransaction()`, `Math.min(totalRisk, 100)` | `TC-RSK-08` | `RiskEngineServiceTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-09** | Risk categorization: LOW (0-29), MEDIUM (30-59), HIGH (60-79), CRITICAL (80-100) | `RiskEngineService.categorizeRisk()` | `TC-RSK-09` | `RiskEngineServiceTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-10** | Single transaction on-demand risk analysis API | `RiskAnalysisController.analyzeSingle()`, `POST /api/risk/analyze/{id}` | `TC-RSK-10` | `RiskAnalysisControllerTest.java` (PASS) | `VERIFIED` |
| **FR-RISK-11** | Batch transaction risk analysis execution | `RiskAnalysisController.analyzeBatch()`, `POST /api/risk/analyze-batch` | `TC-RSK-11` | `RiskAnalysisControllerTest.java` (PASS) | `VERIFIED` |

---

### 2.5 Audit Decision & Governance (`FR-AUDIT`)

| Req ID | Requirement Description | Implemented Classes / Methods / Endpoints | Test Case ID | Test Artifact / Execution Evidence | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **FR-AUDIT-01** | Record audit decision (`APPROVED`, `REJECTED`, `FLAGGED_FOR_REVIEW`) | `AuditController.recordDecision()`, `AuditService.recordDecision()`, `POST /api/audit/decisions` | `TC-AUD-01` | `AuditControllerTest.java`, `AuditServiceTest.java` (PASS) | `VERIFIED` |
| **FR-AUDIT-02** | Mandatory non-empty rationale on all audit decisions | `AuditDecisionRequest` validation, `AuditService.recordDecision()` | `TC-AUD-02` | `AuditServiceTest.java` validating blank rationale rejection (PASS) | `VERIFIED` |
| **FR-AUDIT-03** | Auto-binding auditor identity from authenticated JWT principal | `AuditService.recordDecision()`, `UserPrincipal.getId()` | `TC-AUD-03` | `AuditServiceTest.java` (PASS) | `VERIFIED` |
| **FR-AUDIT-04** | Dual-write atomic transaction: update `audit_decisions` and append `audit_events` | `AuditService.recordDecision()`, `@Transactional`, plain JDBC repositories | `TC-AUD-04` | `AuditServiceTest.java`, `AuditIntegrationTest.java` (PASS) | `VERIFIED` |
| **FR-AUDIT-05** | Append-only audit event history retrieval (application-level; no UPDATE/DELETE APIs exposed) | `AuditController.getAuditEvents()`, `GET /api/audit/events` | `TC-AUD-05` | `AuditControllerTest.java` (PASS) | `VERIFIED` |
| **FR-AUDIT-06** | Audit decision retrieval by transaction ID | `AuditController.getDecisionByTransaction()`, `GET /api/audit/decisions/transaction/{id}` | `TC-AUD-06` | `AuditControllerTest.java` (PASS) | `VERIFIED` |

---

### 2.6 Dashboard & Audit Metrics (`FR-METRIC`)

| Req ID | Requirement Description | Implemented Classes / Methods / Endpoints | Test Case ID | Test Artifact / Execution Evidence | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **FR-METRIC-01** | Overview aggregate counts: total txns, audited txns, pending reviews, high-risk counts | `DashboardController.getOverviewMetrics()`, `DashboardService.getMetrics()`, `GET /api/dashboard/overview` | `TC-DSH-01` | `DashboardControllerTest.java` (PASS) | `VERIFIED` |
| **FR-METRIC-02** | Risk distribution breakdown across LOW, MEDIUM, HIGH, CRITICAL tiers | `DashboardController.getRiskDistribution()`, `GET /api/dashboard/risk-distribution` | `TC-DSH-02` | `DashboardControllerTest.java` (PASS) | `VERIFIED` |
| **FR-METRIC-03** | Temporal audit activity and decision trend statistics | `DashboardController.getDecisionBreakdown()`, `GET /api/dashboard/decisions-summary` | `TC-DSH-03` | `DashboardControllerTest.java` (PASS) | `VERIFIED` |

---

## 3. Non-Functional Requirements Traceability Matrix

| Req ID | Requirement Description | Architectural Implementation Mechanism | Test Case ID | Test Artifact / Execution Evidence | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NFR-SEC-01** | Salted cryptographic password storage | `BCryptPasswordEncoder` (strength 10) in `SecurityConfig` | `TC-SEC-01` | Verified via `UserRepositoryTest.java` (PASS) | `VERIFIED` |
| **NFR-SEC-02** | Stateless token authentication with HMAC-SHA256 | JJWT library (`io.jsonwebtoken:jjwt-api:0.12.6`) | `TC-SEC-02` | Verified via `JwtTokenProviderTest.java` (PASS) | `VERIFIED` |
| **NFR-SEC-03** | Server-side role enforcement (RBAC) | Spring Security HTTP authorizeHttpRequests configuration | `TC-SEC-03` | Verified via `SecurityAccessTest.java` (PASS) | `VERIFIED` |
| **NFR-SEC-04** | Immediate revocation on account deactivation | `JwtAuthenticationFilter` live DB lookup on every request | `TC-SEC-04` | Verified via `JwtAuthenticationFilterTest.java` (PASS) | `VERIFIED` |
| **NFR-PERF-01** | Fast single transaction analysis execution (target sub-100ms; not formally benchmarked) | Deterministic in-memory rule engine execution; indexed SQL queries | `TC-PERF-01` | Automated test suite execution: 175 tests in 4m 39s (suite timing, not per-request benchmark) | `PARTIALLY VERIFIED` |
| **NFR-PERF-02** | High-throughput batch ingestion | Plain JDBC `batchUpdate` with configurable chunk size | `TC-CSV-02` | Batch insert verified in `TransactionRepositoryTest.java` (PASS) | `VERIFIED` |
| **NFR-PERF-03** | Optimized database queries | B-Tree indexes on `tx_timestamp`, `employee_id`, `risk_score` | `TC-DB-01` | Schema verified in `schema.sql` (PASS) | `VERIFIED` |
| **NFR-REL-01** | Atomic dual-write audit logging | Spring `@Transactional` on `AuditService.recordDecision()` | `TC-REL-01` | Atomicity test in `AuditServiceTest.java` (PASS) | `VERIFIED` |
| **NFR-REL-02** | Containerized reproducible execution | Multi-stage Dockerfile and Docker Compose topology | `TC-DEP-01` | `docker-compose.yml` configuration validated (PASS) | `VERIFIED` |
| **NFR-REL-03** | Automated health checks | Spring Boot Actuator `/actuator/health` and Docker healthchecks | `TC-DEP-02` | Actuator configuration verified (PASS) | `VERIFIED` |
| **NFR-COMP-01** | Append-only audit event log (application-enforced; no database-level immutability constraint) | Append-only `audit_events` table; no UPDATE/DELETE operations in `AuditEventRepository` | `TC-AUD-05` | Repository lacks delete/update queries (PASS) | `VERIFIED` |
| **NFR-COMP-02** | Deterministic explainable audit findings | `risk_findings` table stores rule name, score, and explanation | `TC-RSK-08` | Stored finding explainability verified (PASS) | `VERIFIED` |

---

## 4. Requirement Verification Summary Statistics

```
========================================================================================
                   AUREX REQUIREMENTS VERIFICATION SUMMARY
========================================================================================
Total Functional Requirements (FR):       35
  - Verified by Automated/Manual Tests:   35 (100%)
  - Partially Verified:                    0 (0%)
  - Planned / Unimplemented:               0 (Roadmap items documented separately in PRD)

Total Non-Functional Requirements (NFR):  12
  - Verified by Architecture/Tests:       11 (91.7%)
  - Partially Verified:                    1 (8.3%)  [NFR-PERF-01: no per-request benchmark]
  - Planned:                               0 (Roadmap items documented separately)

Combined Requirements Traceability:       47 / 47 Tracked
Backend Automated Tests:                 175 Passed, 0 Failed, 0 Skipped
Frontend Automated Tests:                 56 Passed, 0 Failed, 0 Skipped
========================================================================================
```
