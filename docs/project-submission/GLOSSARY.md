# AUREX — Project Glossary & Technical Terminology

| Document Metadata | Details |
| :--- | :--- |
| **Project** | AUREX — Financial Audit Risk Platform |
| **Document Classification** | Technical Glossary & Domain Terminology Dictionary |
| **Academic Context** | BCA Field Project, MIT Arts, Commerce & Science College, Alandi |
| **Status** | Final Approved Reference |

---

## 1. Domain & Financial Auditing Terminology

### Audit Decision
The formal determination recorded by an authorized auditor (`AUDITOR` or `ADMIN`) regarding the compliance of a financial transaction. In AUREX, decisions are constrained to one of three states: `APPROVED`, `REJECTED`, or `FLAGGED_FOR_REVIEW`. Every audit decision requires a non-empty human rationale and automatically captures the auditor's identity and timestamp.

### Audit Trail
A step-by-step, chronological sequence of audit events providing documentary evidence of the sequence of activities that have affected at any time a specific operation, procedure, or event. In AUREX, this is implemented as an append-only ledger in the `audit_events` database table.

### Deterministic Risk Rule
An algorithmic rule whose outcome is completely determined by its input data and predefined mathematical/logical conditions, with zero randomness or heuristic variability. When identical financial transactions are re-evaluated by the AUREX engine, they yield identical risk scores and finding explanations every time.

### Dual-Write Event Logging
An architectural pattern used in AUREX where recording an audit decision atomically performs two database writes within a single `@Transactional` boundary:
1. An update or insert into the mutable `audit_decisions` table (representing current state).
2. An append-only insert into the `audit_events` table (representing chronological history). The application provides no UPDATE or DELETE API for this table, making it append-only at the application level.

### Duplicate Transaction
A financial anomaly where multiple transactions share identical critical attributes (vendor name, employee ID, monetary amount, and expense category) within a narrow temporal window (10 minutes in AUREX). This pattern frequently indicates billing duplication, invoicing errors, or double-reimbursement fraud.

### High-Amount Materiality Threshold
A monetary threshold above which transactions are deemed of high consequence to an organization's financial reporting. In AUREX, any transaction exceeding INR 100,000.00 triggers the `HighAmountRule`, adding 30 risk points.

### Append-Only Event Log
A data store where records can only be appended and read, but the application exposes no route to modify (`UPDATE`) or remove (`DELETE`) them. In AUREX, the `audit_events` repository intentionally omits any update or delete operations, providing an application-level compliance trail. This is distinct from hardware-level Write-Once Read-Many (WORM) storage or cryptographic tamper evidence.

### Risk Finding
An individual violation or anomaly flagged by a specific risk rule during transaction evaluation. A finding records the rule name, the specific risk score contributed, the severity category, and a human-readable explanation of why the threshold was breached.

### Risk Score Aggregation
The process of combining individual finding scores into a unified composite risk score for a transaction. In AUREX, this is computed as the arithmetic sum of all triggered finding scores, capped at an upper limit of 100 (`Math.min(totalRisk, 100)`).

### Round-Amount Anomaly
A heuristic pattern where transactions are billed in exact multiples of large round numbers (e.g., multiples of INR 10,000.00). In forensic accounting, fabricated invoices and unreceipted expense claims statistically exhibit round amounts far more frequently than genuine operational expenses.

### Transaction Velocity
The frequency or rate at which an entity (such as an employee) submits financial disbursements or claims over a specified time window. In AUREX, submitting 3 or more transactions within a 30-minute window triggers the `TransactionVelocityRule`.

### Vendor Concentration
A financial metric evaluating whether an employee's disbursements are disproportionately directed toward a single vendor. In AUREX, when an employee has at least 3 transactions and 70% or more of their total spend is routed to one vendor, the `VendorConcentrationRule` triggers.

---

## 2. Software Architecture & Engineering Terminology

### BCrypt
A password-hashing function designed by Niels Provos and David Mazières based on the Blowfish cipher. It incorporates a cryptographic salt to protect against rainbow table attacks and includes an adaptive work factor (iteration count). In AUREX, BCrypt with strength 10 is used for all password storage.

### B-Tree Index
A self-balancing tree data structure that maintains sorted data and allows searches, sequential access, insertions, and deletions in logarithmic time ($O(\log n)$). PostgreSQL uses B-Trees by default. AUREX creates B-Tree indexes on `tx_timestamp`, `employee_id`, and `risk_score` to optimize high-volume queries.

### Cross-Origin Resource Sharing (CORS)
A security mechanism using HTTP headers that allows a web server to specify which origins are permitted to access its resources. In AUREX, Spring Security explicitly configures allowed origins, methods, and headers for the Vite React frontend.

### Data Flow Diagram (DFD)
A graphical representation of the "flow" of data through an information system. DFDs model system processes, data stores, external entities, and data flows without detailing control logic or sequence timing.
- **Level 0 (Context Diagram)**: Defines the entire system as a single process with external entities.
- **Level 1 Diagram**: Decomposes the system into major functional processes and specific data stores.

### JSON Web Token (JWT)
An open standard (RFC 7519) defining a compact, URL-safe means for representing claims to be transferred between two parties. In AUREX, stateless JWTs signed with HMAC-SHA256 (`io.jsonwebtoken`) authenticate client requests via the `Authorization: Bearer <token>` HTTP header.

### Plain JDBC (Java Database Connectivity)
The core low-level Java API for executing SQL queries and managing database connections. AUREX strictly uses Spring's `JdbcTemplate` with explicit SQL statements and custom `RowMapper` implementations, entirely avoiding Object-Relational Mapping (ORM) frameworks like Hibernate/JPA. This eliminates N+1 query overhead, hidden cache state, and reflection penalties.

### Reverse Proxy
A server that sits in front of one or more web servers, intercepting requests from clients and routing them to the appropriate backend service. In the AUREX production deployment, Nginx functions as a reverse proxy, serving React static assets on `/` and proxying `/api/*` requests to the Spring Boot backend.

### RFC 4180
The formal Internet Engineering Task Force (IETF) specification defining the Common Format and MIME Type for Comma-Separated Values (CSV) files. AUREX's `CsvTransactionParser` strictly complies with RFC 4180 for batch ingestion.

### Role-Based Access Control (RBAC)
An approach to restricting system access to authorized users based on their assigned roles. AUREX enforces three hierarchical roles:
- `ADMIN`: Full platform configuration, user provisioning, role modification, and account status toggling.
- `AUDITOR`: Full operational auditing capabilities, manual transaction creation, CSV import, risk engine triggering, and audit decision recording.
- `VIEWER`: Read-only access to transaction histories, risk findings, audit logs, and analytics dashboards.

### RowMapper
A Spring JDBC interface that maps each row of a database `ResultSet` to a Java domain object. AUREX implements dedicated `RowMapper` instances (e.g., `TransactionRowMapper`, `UserRowMapper`) for deterministic, zero-overhead object creation.

### Single Page Application (SPA)
A web application or website that interacts with the user by dynamically rewriting the current web page with new data from the web server, instead of the default method of a browser loading entire new pages. AUREX uses React 19 with Vite to deliver a responsive client-side SPA.

### Testcontainers
A Java testing library that supports JUnit tests by providing lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container. AUREX executes all 175 backend integration tests against a real PostgreSQL 16 container managed by Testcontainers.

### Unified Modeling Language (UML)
A standardized modeling language consisting of an integrated set of diagrams, developed to help system and software developers for specifying, visualizing, constructing, and documenting the artifacts of software systems. AUREX provides Class Diagrams, Object Diagrams, Sequence Diagrams, and Use Case Diagrams adhering to UML 2.5 standards.
