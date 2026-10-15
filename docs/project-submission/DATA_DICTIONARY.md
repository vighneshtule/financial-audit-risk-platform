# AUREX — Database Data Dictionary

**Document Version:** 1.0  
**Project Name:** AUREX — Financial Audit Risk Platform  
**Academic Context:** BCA Field Project, MIT Arts, Commerce & Science College, Alandi  
**Database Engine:** PostgreSQL 16  
**Primary Source:** [`src/main/resources/schema.sql`](../../src/main/resources/schema.sql)  
**Supplementary Evidence:** Repository queries in `src/main/java/repository/` and `src/main/java/com/vighnesh/user/UserRepository.java`  

---

## 1. Schema Conventions & Types

The AUREX database adheres to Third Normal Form (3NF) relational design principles:
- **Identifier Types:** Primary keys use 64-bit auto-incrementing integers (`BIGSERIAL` / `BIGINT`) or unique external character identifiers (`VARCHAR(50)` for transactions).
- **Monetary Types:** Monetary values use `NUMERIC(15,2)` to prevent floating-point rounding errors.
- **Timestamps:** System events and audit dates use SQL standard `TIMESTAMP` mapped to Java `LocalDateTime`.
- **Integrity Constraints:** Enforced using PostgreSQL foreign keys, unique indexes, and table check constraints.

---

## 2. Table: `users`

### 2.1 Table Summary
- **Description:** Stores system operator accounts, authentication hashes, role authorizations, and activation status.
- **Storage Profile:** High read frequency (consulted on every authenticated HTTP request to verify account status), low write frequency.

### 2.2 Column Definitions

| Column Name | Data Type | Nullable | Default | Keys & Constraints | Business Meaning |
|---|---|---|---|---|---|
| `id` | `BIGSERIAL` | **NO** | `nextval(...)` | `PRIMARY KEY` | Unique synthetic identifier for the user record. |
| `username` | `VARCHAR(100)` | **NO** | None | `UNIQUE` (`idx_users_username`) | Login username used during authentication. Must be globally unique across all active and inactive users. |
| `password_hash` | `VARCHAR(255)` | **NO** | None | None | Cryptographic BCrypt salted password hash. Plaintext is never stored. |
| `role` | `VARCHAR(20)` | **NO** | None | `CHECK (role IN ('ADMIN', 'AUDITOR', 'VIEWER'))` | System authorization role mapped to Spring Security authority strings (`ROLE_ADMIN`, `ROLE_AUDITOR`, `ROLE_VIEWER`). |
| `enabled` | `BOOLEAN` | **NO** | `TRUE` | None | Account status flag. If set to `false`, authentication is refused and active sessions are revoked immediately on the next request. |
| `created_at` | `TIMESTAMP` | **NO** | `CURRENT_TIMESTAMP` | None | Timestamp when the user account was originally provisioned. |
| `updated_at` | `TIMESTAMP` | **NO** | `CURRENT_TIMESTAMP` | None | Timestamp when the user's role, password, or status was last modified. |

### 2.3 Indexes & Optimization
- `users_pkey` (`PRIMARY KEY, btree (id)`)
- `idx_users_username` (`UNIQUE, btree (username)`) — Accelerates $O(1)$ user lookup during login and per-request JWT filter status checks.
- `idx_users_role` (`btree (role)`) — Accelerates administrative filtering and admin-lockout row locking (`findAdminsForUpdate`).

---

## 3. Table: `transactions`

### 3.1 Table Summary
- **Description:** Stores individual financial transaction records ingested from ERP or corporate bank CSV exports.
- **Storage Profile:** High insert volume during CSV import batches; intensive read access during risk analysis and analytics.

### 3.2 Column Definitions

| Column Name | Data Type | Nullable | Default | Keys & Constraints | Business Meaning |
|---|---|---|---|---|---|
| `transaction_id` | `VARCHAR(50)` | **NO** | None | `PRIMARY KEY` | Corporate reference transaction code (e.g. `TXN-001`, `TXN-90214`). Acts as natural primary key. |
| `vendor` | `VARCHAR(255)` | **NO** | None | `idx_transactions_vendor` | Name of the third-party merchant, supplier, or contractor receiving payment. |
| `employee` | `VARCHAR(100)` | **NO** | None | `idx_transactions_employee` | Identifier or name of the corporate employee who authorized or submitted the expense. |
| `amount` | `NUMERIC(15,2)` | **NO** | None | None | Exact financial value of the transaction in INR. |
| `transaction_time` | `TIMESTAMP` | **NO** | None | `idx_transactions_time` | Actual date and time when the financial transaction was authorized or settled. |
| `category` | `VARCHAR(100)` | **NO** | None | `idx_transactions_category` | Corporate accounting expense category *(Inferred: represents organizational budget classification, such as SOFTWARE, HARDWARE, LOGISTICS, TRAVEL)*. |

### 3.3 Indexes & Optimization
- `transactions_pkey` (`PRIMARY KEY, btree (transaction_id)`)
- `idx_transactions_vendor` (`btree (vendor)`) — Enables fast relationship queries for vendor peer transactions (`findRelatedByVendor`).
- `idx_transactions_employee` (`btree (employee)`) — Accelerates velocity sliding-window queries and employee vendor concentration checks.
- `idx_transactions_category` (`btree (category)`) — Accelerates category peer relationship queries and category risk aggregation.
- `idx_transactions_time` (`btree (transaction_time)`) — Optimizes chronological sorting and time-window difference computations.

---

## 4. Table: `risk_analysis_runs`

### 4.1 Table Summary
- **Description:** Represents an execution run of the deterministic risk evaluation engine against a specific transaction.
- **Storage Profile:** High write frequency during batch analysis (`POST /api/risk/analyze-all`); queried heavily by analytical dashboards.

### 4.2 Column Definitions

| Column Name | Data Type | Nullable | Default | Keys & Constraints | Business Meaning |
|---|---|---|---|---|---|
| `id` | `BIGSERIAL` | **NO** | `nextval(...)` | `PRIMARY KEY` | Unique synthetic identifier for the risk analysis run. |
| `transaction_id` | `VARCHAR(50)` | **NO** | None | `FK -> transactions(transaction_id) ON DELETE CASCADE` | Identifier of the transaction evaluated during this run. |
| `risk_score` | `INTEGER` | **NO** | None | None | Total cumulative risk score assigned by the engine (capped between 0 and 100). |
| `risk_level` | `VARCHAR(20)` | **NO** | None | None | Risk severity rating derived from score (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). |
| `analyzed_at` | `TIMESTAMP` | **NO** | `CURRENT_TIMESTAMP` | `idx_analysis_runs_analyzed_at` | Exact system timestamp when the evaluation run was executed. |

### 4.3 Indexes & Optimization
- `risk_analysis_runs_pkey` (`PRIMARY KEY, btree (id)`)
- `idx_analysis_runs_transaction` (`btree (transaction_id)`)
- `idx_analysis_runs_analyzed_at` (`btree (analyzed_at)`)
- `idx_analysis_runs_txn_analyzed` (`btree (transaction_id, analyzed_at DESC, id DESC)`) — Critical composite index optimizing PostgreSQL `DISTINCT ON (transaction_id)` queries fetching the most recent evaluation run.

---

## 5. Table: `risk_findings`

### 5.1 Table Summary
- **Description:** Stores individual policy violations or risk indicators triggered during a risk analysis run.
- **Relational Integrity Note:** The database foreign key is strictly defined on column `analysis_run_id` (`CONSTRAINT fk_finding_analysis_run FOREIGN KEY (analysis_run_id) REFERENCES risk_analysis_runs(id) ON DELETE CASCADE`). There is no column named `run_id` in the database schema; references to `run_id` in code or DTOs represent an application-managed convenience mapping.
- **Storage Profile:** Multi-row insert per evaluated transaction; queried when rendering the transaction investigation workspace.

### 5.2 Column Definitions

| Column Name | Data Type | Nullable | Default | Keys & Constraints | Business Meaning |
|---|---|---|---|---|---|
| `id` | `BIGSERIAL` | **NO** | `nextval(...)` | `PRIMARY KEY` | Unique synthetic finding identifier. |
| `analysis_run_id` | `BIGINT` | **NO** | None | `FK -> risk_analysis_runs(id) ON DELETE CASCADE` | Parent analysis run that detected this finding. |
| `transaction_id` | `VARCHAR(50)` | **NO** | None | `FK -> transactions(transaction_id) ON DELETE CASCADE` | Target transaction that violated the audit policy. |
| `risk_type` | `VARCHAR(100)` | **NO** | None | `idx_risk_findings_type` | Specific policy rule breached (`HIGH_AMOUNT`, `UNUSUAL_TRANSACTION_TIME`, `DUPLICATE_TRANSACTION`, `ROUND_AMOUNT`, `TRANSACTION_VELOCITY`, `VENDOR_CONCENTRATION`). |
| `score` | `INTEGER` | **NO** | None | None | Mathematical risk score contributed by this rule (+10, +15, +20, +25, or +30). |
| `severity` | `VARCHAR(20)` | **NO** | None | `idx_risk_findings_severity` | Qualitative severity tag assigned to the finding (defaults to `MEDIUM`). |
| `explanation` | `TEXT` | **NO** | None | None | Plaintext forensic explanation detailing the exact values and thresholds that triggered the rule. |
| `created_at` | `TIMESTAMP` | **NO** | `CURRENT_TIMESTAMP` | None | Timestamp when the finding was recorded. |

### 5.3 Indexes & Optimization
- `risk_findings_pkey` (`PRIMARY KEY, btree (id)`)
- `idx_risk_findings_analysis_run` (`btree (analysis_run_id)`) — Enables instant lookup of findings by run ID (`findByAnalysisRunId`).
- `idx_risk_findings_transaction` (`btree (transaction_id)`)
- `idx_risk_findings_type` (`btree (risk_type)`) — Accelerates aggregate finding distribution queries in `AnalyticsRepository`.
- `idx_risk_findings_severity` (`btree (severity)`)

---

## 6. Table: `audit_decisions`

### 6.1 Table Summary
- **Description:** Stores formal compliance decisions recorded by internal auditors or compliance managers.
- **Storage Profile:** Low write volume; high compliance durability requirement (`ON DELETE RESTRICT` protects records against accidental deletion).

### 6.2 Column Definitions

| Column Name | Data Type | Nullable | Default | Keys & Constraints | Business Meaning |
|---|---|---|---|---|---|
| `id` | `BIGSERIAL` | **NO** | `nextval(...)` | `PRIMARY KEY` | Unique synthetic identifier for the formal audit decision. |
| `transaction_id` | `VARCHAR(50)` | **NO** | None | `FK -> transactions(transaction_id) ON DELETE RESTRICT` | Reference to the audited transaction. |
| `analysis_run_id` | `BIGINT` | **NO** | None | `FK -> risk_analysis_runs(id) ON DELETE RESTRICT` | Specific evaluation run verified by the auditor. |
| `decision` | `VARCHAR(40)` | **NO** | None | None | Standardized decision determination: `CONFIRMED_RISK`, `FALSE_POSITIVE`, `REQUIRES_INVESTIGATION`, `ESCALATED`. |
| `comment` | `TEXT` | **YES** | None | None | Narrative investigative commentary entered by the auditor (up to 2,000 characters). |
| `decided_by` | `VARCHAR(100)` | **YES** | None | *(Application-enforced logical link to `users.username`)* | Authenticated username of the auditor who recorded the decision. Sourced strictly from Spring Security `Principal`. |
| `decided_at` | `TIMESTAMP` | **NO** | `CURRENT_TIMESTAMP` | `idx_audit_decisions_decided_at` | Exact system timestamp when the decision was recorded. |

### 6.3 Indexes & Optimization
- `audit_decisions_pkey` (`PRIMARY KEY, btree (id)`)
- `idx_audit_decisions_transaction` (`btree (transaction_id)`)
- `idx_audit_decisions_analysis_run` (`btree (analysis_run_id)`)
- `idx_audit_decisions_decided_at` (`btree (decided_at DESC, id DESC)`) — Optimizes reverse-chronological decision retrieval.

---

## 7. Table: `audit_events`

### 7.1 Table Summary
- **Description:** Stores append-only chronological audit log entries triggered by compliance actions.
- **Storage Profile:** Append-only log at the application layer; repository and service expose only insert and read methods, without update or delete APIs. Note that database-level cryptographic immutability, tamper-proof hardware (WORM), or row-level update-blocking triggers are not implemented.

### 7.2 Column Definitions

| Column Name | Data Type | Nullable | Default | Keys & Constraints | Business Meaning |
|---|---|---|---|---|---|
| `id` | `BIGSERIAL` | **NO** | `nextval(...)` | `PRIMARY KEY` | Unique event log identifier. |
| `transaction_id` | `VARCHAR(50)` | **NO** | None | `FK -> transactions(transaction_id) ON DELETE RESTRICT` | Financial transaction referenced by this event log. |
| `decision_id` | `BIGINT` | **YES** | None | `FK -> audit_decisions(id) ON DELETE RESTRICT` | Specific decision that triggered this event (nullable for non-decision events). |
| `event_type` | `VARCHAR(50)` | **NO** | None | None | Categorical event classification code (e.g. `AUDIT_DECISION_CREATED`). |
| `event_details` | `TEXT` | **YES** | None | None | Detailed audit trail message describing the action taken and affected parameters. |
| `actor` | `VARCHAR(100)` | **YES** | None | *(Application-enforced logical link to `users.username`)* | Verified authenticated username responsible for generating the event. |
| `created_at` | `TIMESTAMP` | **NO** | `CURRENT_TIMESTAMP` | `idx_audit_events_created_at` | Timestamp when the event record was created. |

### 7.3 Indexes & Optimization
- `audit_events_pkey` (`PRIMARY KEY, btree (id)`)
- `idx_audit_events_transaction` (`btree (transaction_id)`)
- `idx_audit_events_decision` (`btree (decision_id)`)
- `idx_audit_events_created_at` (`btree (created_at DESC, id DESC)`)
