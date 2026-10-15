# AUREX — Product Requirements Document (PRD)

**Document Version:** 1.0  
**Project Name:** AUREX — Financial Audit Risk Platform  
**Academic Context:** BCA Field Project, MIT Arts, Commerce & Science College, Alandi  
**Author / Candidate:** Vighnesh Tule  
**Status:** Verified Against Active Implementation  

---

## 1. Executive Summary

AUREX is a specialized financial audit and risk intelligence web application engineered to evaluate corporate financial transactions against deterministic audit policies, detect risk velocity anomalies, track historical evaluation runs, and provide cross-entity investigative evidence across vendor, employee, and category dimensions. 

Corporate financial auditing traditionally relies on manual sample checking or disconnected spreadsheets, leading to undetected policy breaches, duplicate vendor payments, off-hours transaction anomalies, and unmonitored employee spending concentration. AUREX addresses this operational challenge by ingesting transaction streams and deterministically assessing every transaction against multi-dimensional audit rules with persisted historical audit trails and forensic decision logging.

The system is developed using Java 17 and Spring Boot 3.5.6 on the backend with plain JDBC persistence (avoiding ORM overhead) connected to PostgreSQL 16. The user interface is built as a single-page application using React 19, TypeScript, Vite, and Tailwind CSS.

---

## 2. Problem Statement & Background

### 2.1 The Operational Challenge
In corporate accounting and internal audit environments, financial transactions are typically inspected post-settlement. Traditional methods face several structural flaws:
1. **Sampling Bias**: Auditors frequently inspect only a 5% to 10% random sample of transactions, allowing systemic anomalies (e.g., duplicate payments or off-hours transfers) to bypass inspection.
2. **Disconnected Evidence**: Identifying fraud or policy violations often requires cross-referencing multiple disparate dimensions (e.g., employee spending frequency, vendor concentration, round-amount transfers), which is tedious in conventional spreadsheets.
3. **Lack of Audit Reproducibility**: When a transaction is reviewed, the exact policy criteria and scoring at the time of evaluation are rarely preserved, making historical audit defense difficult.
4. **Vulnerable Audit Logging**: In informal tracking sheets, audit decisions can be retroactively altered without verifiable identity tracking.

### 2.2 The AUREX Solution
AUREX solves these problems by providing:
- **100% Ingestion Coverage**: Every transaction in an imported batch is parsed and validated.
- **Deterministic Risk Scoring**: Transparent, reproducible rule evaluation with mathematical scoring (0–100) and severity classification.
- **Cross-Entity Evidence Context**: Automatic correlation of transactions across common vendors, employees, and categories.
- **Append-Only Audit Trail**: Forensic decision logging where the deciding actor is strictly derived from the authenticated security principal. Audit events are append-only at the application layer (no UPDATE/DELETE APIs); cryptographic or hardware-level immutability is not provided.

---

## 3. Project Goals & Measurable Acceptance Criteria

| Goal ID | Objective | Measurable Acceptance Criteria |
|---|---|---|
| **G-01** | High-throughput Ingestion | Ingest standard CSV transaction batches (RFC 4180) containing up to 50MB files atomically within a single database transaction. |
| **G-02** | Deterministic Risk Evaluation | Evaluate 100% of imported transactions against 6 independent, configurable audit policies, producing a score between 0 and 100. |
| **G-03** | Forensic Audit Trail | Record every auditor decision with an append-only audit event log where the recorded actor is verified against the authenticated security principal. |
| **G-04** | Role-Based Access Control | Enforce strict separation between `ADMIN`, `AUDITOR`, and `VIEWER` roles at both HTTP routing and database mutation layers. |
| **G-05** | Zero ORM Overhead | Implement all database interactions using plain JDBC (`JdbcTemplate`, `DataSourceUtils`, `PreparedStatement`) to ensure deterministic SQL execution. |

---

## 4. Intended Users & Role Definitions

The system defines three mutually exclusive security roles:

| Role Name | Authority String | Target User | Responsibilities & Access Rights |
|---|---|---|---|
| **Compliance Administrator** | `ROLE_ADMIN` | IT & Compliance Operations Head | User lifecycle management (create accounts, toggle status); system configuration; full operational access to CSV ingestion, risk analysis, and audit decisions. Protected by self-disable and last-admin lockout rules. |
| **Internal Auditor** | `ROLE_AUDITOR` | Senior Auditor / Fraud Investigator | Imports CSV transaction batches; triggers single or batch risk evaluations; reviews forensic evidence graphs; records formal audit decisions (`CONFIRMED_RISK`, `FALSE_POSITIVE`, `REQUIRES_INVESTIGATION`, `ESCALATED`). Forbidden from user management. |
| **Audit Viewer** | `ROLE_VIEWER` | Executive / Compliance Observer | Read-only observer. Can browse transactions, inspect risk scores, view findings, review decision history, and inspect executive risk intelligence summaries. Forbidden from mutations (upload, analysis trigger, decision recording, user management). |

---

## 5. Functional Requirements (FR)

### 5.1 Authentication & User Management
- **FR-001**: The system shall provide a password-authenticated login endpoint (`POST /api/auth/login`) that validates credentials against BCrypt salted password hashes.
- **FR-002**: The system shall generate a cryptographically signed HMAC-SHA256 JWT containing the user's username, database ID, and assigned role upon successful authentication.
- **FR-003**: The system shall enforce per-request account status verification: if a user's `enabled` flag is toggled to `false` in the database, subsequent requests using existing JWTs must immediately receive HTTP 401 Unauthorized.
- **FR-004**: The system shall allow administrators to view all registered users (`GET /api/users`), create new users (`POST /api/users`), and toggle user status (`PATCH /api/users/{id}/status`).
- **FR-005**: The system shall prevent an administrator from disabling their own currently authenticated account.
- **FR-006**: The system shall prevent the last remaining enabled `ADMIN` account from being disabled using pessimistic database row locking (`SELECT ... FOR UPDATE`).

### 5.2 Transaction Ingestion
- **FR-007**: The system shall accept multipart CSV file uploads (`POST /api/transactions/import`) with a configurable size limit of up to 50MB.
- **FR-008**: The system shall validate that the uploaded file is non-empty and possesses a valid `.csv` extension.
- **FR-009**: The system shall parse CSV columns (`transaction_id`, `vendor`, `employee`, `amount`, `transaction_time`, `category`) and execute batch insertion atomically with `connection.setAutoCommit(false)`. If any row fails, the entire batch must roll back.
- **FR-010**: The system shall provide a query endpoint (`GET /api/transactions`) to retrieve all ingested transactions ordered chronologically.

### 5.3 Deterministic Risk Engine
- **FR-011**: The system shall evaluate transactions against the **High Amount Policy**: flags transactions with `amount > 100,000` INR (Score: +30, Severity: `MEDIUM`).
- **FR-012**: The system shall evaluate transactions against the **Unusual Time Policy**: flags transactions occurring outside business hours (`09:00` to `18:00`) (Score: +20, Severity: `MEDIUM`).
- **FR-013**: The system shall evaluate transactions against the **Round Amount Policy**: flags transactions where `amount > 0` and `amount % 10,000 == 0` (Score: +10, Severity: `MEDIUM`).
- **FR-014**: The system shall evaluate transactions against the **Transaction Velocity Policy**: flags an employee if they have $\ge 3$ transactions within a 30-minute sliding window (Score: +20, Severity: `MEDIUM`).
- **FR-015**: The system shall evaluate transactions against the **Duplicate Transaction Policy**: flags transactions matching another record on `(vendor, employee, amount, category)` with a different ID within 10 minutes (Score: +25, Severity: `MEDIUM`).
- **FR-016**: The system shall evaluate transactions against the **Vendor Concentration Policy**: flags an employee if they have $\ge 3$ transactions and $\ge 70.0\%$ of their total transactions are concentrated with a single vendor (Score: +15, Severity: `MEDIUM`).
- **FR-017**: The system shall compute an aggregated Risk Score as the sum of all triggered policy scores capped at 100: $\text{Score} = \min(\sum \text{Scores}, 100)$.
- **FR-018**: The system shall categorize risk severity into discrete tiers: `CRITICAL` ($\ge 80$), `HIGH` ($60 - 79$), `MEDIUM` ($30 - 59$), and `LOW` ($0 - 29$).
- **FR-019**: The system shall provide on-demand live evaluation (`GET /api/risk/transactions/{id}`) without mandatory database writes.
- **FR-020**: The system shall support persistent single-transaction evaluation (`POST /api/risk/analyze/{id}`) and batch evaluation (`POST /api/risk/analyze-all`), saving run records to `risk_analysis_runs` and individual findings to `risk_findings`.
- **FR-021**: The system shall provide paginated and filtered views of evaluated transactions (`GET /api/risk/transactions`) supporting filtering by `riskLevel` and `minScore`.

### 5.4 Forensic Evidence & History
- **FR-022**: The system shall provide cross-entity relationship context (`GET /api/risk/transactions/{id}/context`), querying peer transactions sharing the same vendor, employee, and category, reporting aggregate transaction counts, sum values, and peak risk scores.
- **FR-023**: The system shall record and retrieve historical evaluation runs per transaction (`GET /api/risk/transactions/{id}/history` and paginated `.../history/page`) to track risk evolution across re-runs.

### 5.5 Audit Decisions & Event Trail
- **FR-024**: The system shall allow authorized auditors to record an audit decision (`POST /api/risk/transactions/{id}/audit/decisions`) selecting from `CONFIRMED_RISK`, `FALSE_POSITIVE`, `REQUIRES_INVESTIGATION`, or `ESCALATED`.
- **FR-025**: The system shall strictly source the decision `decided_by` actor and the audit event `actor` from the authenticated security principal (`Principal.getName()`), discarding any client-submitted actor value to prevent audit log falsification.
- **FR-026**: The system shall automatically generate an append-only audit event in `audit_events` whenever an audit decision is created. The application exposes no UPDATE or DELETE operation on this table.
- **FR-027**: The system shall provide audit history endpoints (`GET .../audit/decisions` and `GET .../audit/events`) to review all historical decisions and events.

### 5.6 Executive Analytics & System Health
- **FR-028**: The system shall compute multidimensional risk intelligence summaries (`GET /api/risk/intelligence/summary` and `GET /api/risk/summary`), aggregating dataset metrics, severity counts, vendor concentration, employee risk profiles, category distributions, and rule finding frequencies.
- **FR-029**: The system shall provide a public health check endpoint (`GET /api/health`) reporting system status and active database connectivity.

---

## 6. Non-Functional Requirements (NFR)

- **NFR-001 (Performance)**: The deterministic risk engine shall evaluate a batch of 1,000 transactions in under 2 seconds in memory.
- **NFR-002 (Security - Password Storage)**: Passwords shall be hashed using BCrypt with automatic salting before persistence. Plaintext passwords shall never be logged or returned in API responses.
- **NFR-003 (Security - Token Integrity)**: JWT tokens shall be signed using HMAC-SHA256 with a minimum 256-bit (32 character) secret key. Production startup must fail immediately if default or short keys are detected.
- **NFR-004 (Reliability & Atomicity)**: CSV ingestion and batch risk persistence shall be strictly atomic. Failure in any single record must trigger a complete rollback of the current transaction.
- **NFR-005 (Data Integrity - Cascade Deletion)**: Deletion of a transaction must cascade to its `risk_analysis_runs` and `risk_findings`, while audit decisions and events must enforce `ON DELETE RESTRICT` to prevent accidental loss of compliance records.
- **NFR-006 (Maintainability - Zero ORM Overhead)**: The persistence layer shall use plain JDBC without JPA or Hibernate to eliminate N+1 query problems, implicit entity dirty-checking, and non-deterministic SQL generation.
- **NFR-007 (Usability - Responsive UI)**: The frontend single-page application shall provide responsive views suitable for desktop displays ($\ge 1280\text{px}$ standard audit workstation resolution) with dark/light theme switching.

---

## 7. User Stories & Acceptance Criteria

### User Story 1: Ingesting Daily Transactions
> **As an** Internal Auditor  
> **I want to** upload a batch CSV file of daily company transactions  
> **So that** they are validated, mapped, and stored in the audit database.

- **Acceptance Criteria:**
  1. The auditor navigates to `/import`, selects a `.csv` file, and clicks upload.
  2. If the file is non-CSV or empty, a descriptive HTTP 400 error message is displayed.
  3. On success, the total number of imported records is displayed, and transactions immediately appear in the `/transactions` view.

### User Story 2: Detecting High-Risk Transactions
> **As an** Internal Auditor  
> **I want to** run deterministic risk analysis across all ingested transactions  
> **So that** high-risk anomalies (e.g., duplicate payments, round amounts, off-hours transfers) are prioritized for review.

- **Acceptance Criteria:**
  1. The auditor triggers batch analysis via `/api/risk/analyze-all`.
  2. Transactions with scores $\ge 60$ are marked with `HIGH` or `CRITICAL` badges.
  3. Clicking a transaction opens `/transactions/:id`, detailing exact triggered findings with explanations and scores.

### User Story 3: Recording an Audit Decision
> **As an** Internal Auditor  
> **I want to** record a formal audit decision with notes on an investigated transaction  
> **So that** compliance records reflect the outcome of the investigation.

- **Acceptance Criteria:**
  1. The auditor selects an action (e.g., `REQUIRES_INVESTIGATION`) and types an investigative note.
  2. Submitting saves the decision and produces an append-only event record (application-level; not cryptographically signed).
  3. The recorded actor reflects the logged-in auditor's username, preventing impersonation.

### User Story 4: Administrator User Lifecycle Control
> **As a** Compliance Administrator  
> **I want to** provision new auditor accounts and disable inactive accounts  
> **So that** only authorized personnel can access corporate audit data.

- **Acceptance Criteria:**
  1. The administrator visits `/users` and creates a new `AUDITOR` user with a temporary password $\ge 6$ characters.
  2. The administrator can disable an account; disabled users are immediately blocked from logging in.
  3. If an administrator attempts to disable their own account or the last enabled admin, the system rejects the operation with HTTP 409 Conflict.

---

## 8. In-Scope vs. Out-of-Scope Capabilities

### In-Scope (Implemented & Verified)
- Multi-row CSV parsing and atomic plain JDBC ingestion.
- 6-policy deterministic risk engine with capped score calculation.
- Historical evaluation run persistence and comparison.
- Cross-entity evidence context graphs (vendor, employee, category).
- Role-based access control (`ADMIN`, `AUDITOR`, `VIEWER`) via JWT.
- Append-only audit decision and event history tracking (application-enforced).
- Self-disable and last-admin lockout protections.
- Multidimensional risk intelligence analytics and charts.
- Multi-container Docker deployment (Postgres, Spring Boot, React on Nginx).

### Out-of-Scope (Future Enhancements / Non-Implemented)
- Generative AI / Large Language Model narrative summaries.
- Generic Kanban/Jira-style case management ticket lifecycles (`OPEN`, `IN_REVIEW`, `RESOLVED`).
- Automated multi-currency foreign exchange conversion.
- Live streaming ingestion via Apache Kafka or RabbitMQ.
- Two-factor SMS/TOTP authentication.

---

## 9. Business Rules & Operational Constraints

1. **Scoring Bounds**: The cumulative risk score cannot exceed 100, regardless of the number of triggered policies.
2. **Audit Actor Integrity**: The audit decision actor cannot be specified by the HTTP client; it is strictly populated from the authenticated session principal. This is an application-level control.
3. **Admin Resilience**: The system must never enter a state where zero active administrators exist.
4. **Immediate Revocation**: Disabling a user account terminates access on the subsequent HTTP request, even if the user's JWT has not reached its 24-hour expiration.
5. **Deterministic Results**: Given an identical transaction dataset, the deterministic risk engine must yield identical scores and findings across all runs.
