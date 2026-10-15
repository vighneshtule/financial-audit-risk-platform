# AUREX — System Requirements Specification (SRS)

**Document Version:** 1.0  
**Project Name:** AUREX — Financial Audit Risk Platform  
**Academic Context:** BCA Field Project, MIT Arts, Commerce & Science College, Alandi  
**Author / Candidate:** Vighnesh Tule  
**Status:** Verified Against Active Implementation  

---

## 1. Introduction

This System Requirements Specification (SRS) defines the complete software, hardware, functional, and non-functional specifications for the **AUREX Financial Audit Risk Platform**. Every requirement defined herein maps directly to implemented components, database structures, or automated test cases in the active repository.

---

## 2. Environment & System Prerequisites

### 2.1 Hardware Requirements

#### Minimum Recommended Development / Evaluation Workstation:
- **Processor:** Intel Core i5 / AMD Ryzen 5 (4 cores, 8 threads or higher)
- **Memory (RAM):** 8 GB minimum (16 GB recommended to support Docker Desktop and IDE simultaneously)
- **Storage:** 5 GB free SSD storage (accommodates source code, Maven repository, node_modules, and Docker container images)
- **Network:** Standard localhost loopback adapter; internet access required only for dependency downloading and image pulling.

#### Minimum Deployment Container Footprint:
- **Frontend Container (Nginx Alpine):** 64 MB RAM, 0.1 CPU core
- **Backend Container (Temurin 17 JRE Alpine):** 512 MB RAM, 0.5 CPU core
- **Database Container (Postgres 16 Alpine):** 256 MB RAM, 0.5 CPU core

### 2.2 Software Prerequisites

| Component | Minimum Version | Verified Active Version | Notes |
|---|---|---|---|
| **Operating System** | Windows 10/11, Ubuntu 22.04 LTS, or macOS 13+ | Windows 11 (build 10.0, 64-bit) | Containerized deployment is OS-agnostic |
| **Java Development Kit** | JDK 17 LTS | OpenJDK `17.0.19 LTS` (Microsoft HotSpot) | Required for backend compilation and test execution |
| **Build Tool (Backend)** | Apache Maven 3.8+ | Apache Maven `3.9.16` | Required for building backend JAR |
| **Node.js & npm** | Node.js 20 LTS / npm 10+ | Node.js `v24.11.1` / npm `11.6.2` | Required for frontend TypeScript compilation & testing |
| **Database Server** | PostgreSQL 15+ | PostgreSQL `16.0` (Dockerized `16-alpine`) | Uses standard TCP port 5432 |
| **Containerization** | Docker Engine 24+ & Docker Compose v2+ | Docker Engine 29.1.3 / Compose v2 | Required for multi-container stack deployment and Testcontainers |

---

## 3. Functional Requirements Catalog

### 3.1 Authentication & Authorization Module (`FR-AUTH`)

- **FR-AUTH-01 (Credential Authentication)**: The system shall accept a JSON body containing `username` and `password` on `POST /api/auth/login`, verifying credentials against BCrypt password hashes stored in `users.password_hash`.
- **FR-AUTH-02 (JWT Generation)**: Upon valid authentication, the system shall issue a signed HMAC-SHA256 JWT containing `sub` (username), `id` (user primary key), `role` (`ADMIN`, `AUDITOR`, or `VIEWER`), and an expiration timestamp (default: 86,400 seconds / 24 hours).
- **FR-AUTH-03 (Stateless Token Validation)**: The system shall validate the `Authorization: Bearer <token>` header on every protected request in `JwtAuthenticationFilter`.
- **FR-AUTH-04 (Live Disabled Account Check)**: On every incoming request containing a valid JWT, the filter shall query `UserRepository.findByUsername` to verify that `user.isEnabled() == true`. If `enabled` is false, the request must be rejected immediately with HTTP 401 Unauthorized.
- **FR-AUTH-05 (Current Identity Endpoint)**: The system shall expose `GET /api/auth/me`, returning the authenticated caller's `UserDto` (sanitizing password fields).
- **FR-AUTH-06 (Role-Based Authorization Matrix)**:
  - Endpoints under `/api/users/**` shall be accessible exclusively by users with authority `ROLE_ADMIN`.
  - Mutation endpoints `POST /api/transactions/import`, `POST /api/risk/analyze/**`, and `POST /api/risk/.../audit/decisions` shall be accessible exclusively by users with authority `ROLE_ADMIN` or `ROLE_AUDITOR`.
  - Read-only endpoints (`GET /api/transactions`, `GET /api/risk/**`) shall be accessible by any authenticated user (`ADMIN`, `AUDITOR`, `VIEWER`).
  - Endpoints `/api/auth/login`, `/api/health`, `/v3/api-docs/**`, and `/swagger-ui/**` shall be publicly accessible without authentication.

### 3.2 User Lifecycle Administration Module (`FR-USER`)

- **FR-USER-01 (User Directory Listing)**: Administrators shall be able to fetch all user accounts via `GET /api/users`.
- **FR-USER-02 (User Account Provisioning)**: Administrators shall be able to create new accounts via `POST /api/users` with non-blank `username`, `password` ($\ge 6$ characters), and valid `role` (`ADMIN`, `AUDITOR`, `VIEWER`). Duplicate usernames must be rejected with HTTP 409 Conflict.
- **FR-USER-03 (Account Status Toggling)**: Administrators shall be able to enable or disable accounts via `PATCH /api/users/{id}/status`.
- **FR-USER-04 (Self-Disable Protection)**: An administrator shall be forbidden from disabling their own currently authenticated account; attempts must be rejected with HTTP 409 Conflict.
- **FR-USER-05 (Last-Admin Lockout Guard)**: The system shall prevent the last remaining enabled `ADMIN` account from being disabled using pessimistic database row locking (`SELECT ... FOR UPDATE`); attempts must be rejected with HTTP 409 Conflict.

### 3.3 Transaction Ingestion Module (`FR-INGEST`)

- **FR-INGEST-01 (Multipart CSV Upload)**: The system shall accept multipart file uploads up to 50MB on `POST /api/transactions/import`.
- **FR-INGEST-02 (File Format Validation)**: The system shall reject missing, empty, or non-CSV files with HTTP 400 Bad Request.
- **FR-INGEST-03 (Atomic Batch Commit)**: Ingestion shall use `connection.setAutoCommit(false)` in `TransactionImportService`. If any transaction violates constraints, the entire batch shall roll back.
- **FR-INGEST-04 (Transaction Listing)**: The system shall return all stored transactions ordered chronologically on `GET /api/transactions`.

### 3.4 Deterministic Risk Engine Module (`FR-RISK`)

- **FR-RISK-01 (High Amount Rule)**: Flags any transaction with `amount > 100,000` INR with Score: 30, Severity: `MEDIUM`.
- **FR-RISK-02 (Unusual Time Rule)**: Flags any transaction with `transaction_time` before `09:00` or after `18:00` with Score: 20, Severity: `MEDIUM`.
- **FR-RISK-03 (Round Amount Rule)**: Flags any transaction with `amount > 0` and `amount % 10,000 == 0` with Score: 10, Severity: `MEDIUM`.
- **FR-RISK-04 (Transaction Velocity Rule)**: Flags transactions where the same employee has $\ge 3$ transactions within a 30-minute window with Score: 20, Severity: `MEDIUM`.
- **FR-RISK-05 (Duplicate Transaction Rule)**: Flags transactions sharing identical `(vendor, employee, amount, category)` with a different ID within 10 minutes with Score: 25, Severity: `MEDIUM`.
- **FR-RISK-06 (Vendor Concentration Rule)**: Flags transactions where an employee has $\ge 3$ transactions and $\ge 70.0\%$ of their transactions are concentrated with a single vendor with Score: 15, Severity: `MEDIUM`.
- **FR-RISK-07 (Score Calculation & Capping)**: Calculates cumulative score capped at 100: $\text{Score} = \min(\sum \text{Scores}, 100)$.
- **FR-RISK-08 (Severity Mapping)**: Maps scores: $\ge 80 \to \text{CRITICAL}$, $60-79 \to \text{HIGH}$, $30-59 \to \text{MEDIUM}$, $0-29 \to \text{LOW}$.
- **FR-RISK-09 (Single-Transaction Live Evaluation)**: Evaluates a transaction in memory on `GET /api/risk/transactions/{id}` without writing to DB.
- **FR-RISK-10 (Persistent Evaluation & Findings)**: Evaluates and persists run records into `risk_analysis_runs` and findings into `risk_findings` on `POST /api/risk/analyze/{id}` and `POST /api/risk/analyze-all`.
- **FR-RISK-11 (Filtered & Paginated Results)**: Exposes `GET /api/risk/transactions` supporting query parameters `riskLevel`, `minScore`, `page`, and `size`.

### 3.5 Forensic Evidence & Audit Decision Module (`FR-AUDIT`)

- **FR-AUDIT-01 (Cross-Entity Context Graph)**: Provides peer transactions sharing the same vendor, employee, and category on `GET /api/risk/transactions/{id}/context`.
- **FR-AUDIT-02 (Evaluation Run History)**: Exposes historical runs for a transaction on `GET .../history` and paginated `.../history/page`.
- **FR-AUDIT-03 (Record Audit Decision)**: Records an audit decision (`CONFIRMED_RISK`, `FALSE_POSITIVE`, `REQUIRES_INVESTIGATION`, `ESCALATED`) via `POST .../audit/decisions`.
- **FR-AUDIT-04 (Audit Actor Integrity)**: Sourced strictly from authenticated `Principal.getName()`, preventing client manipulation of `decided_by` or event `actor`.
- **FR-AUDIT-05 (Dual Event Logging)**: Inserts an event record into `audit_events` whenever an audit decision is created within a `@Transactional` block.
- **FR-AUDIT-06 (Audit Trail Retrieval)**: Retrieves decision history on `GET .../audit/decisions` and event history on `GET .../audit/events`.

### 3.6 Analytics & Diagnostics Module (`FR-METRIC`)

- **FR-METRIC-01 (Executive Risk Summary)**: Computes high-level counts and totals on `GET /api/risk/summary`.
- **FR-METRIC-02 (Multidimensional Intelligence Summary)**: Computes dataset overview, severity distribution, vendor risk, employee risk, category risk, and finding distributions on `GET /api/risk/intelligence/summary`.
- **FR-METRIC-03 (Public System Health)**: Returns `{"status":"UP","database":"UP"}` on `GET /api/health` if the PostgreSQL connection is active.

---

## 4. Non-Functional Requirements Catalog

| Requirement ID | Category | Specification |
|---|---|---|
| **NFR-SEC-01** | Security | Passwords must be encoded using BCrypt with automatic salting before persistence. |
| **NFR-SEC-02** | Security | JWT secrets must be at least 32 characters in production; application must fail fast if unconfigured or default. |
| **NFR-SEC-03** | Security | Error responses must not leak stack traces or database schema internals to the client. |
| **NFR-PERF-01** | Performance | CSV ingestion must execute via batch `PreparedStatement` to process $>500$ rows/second. |
| **NFR-PERF-02** | Performance | Analytical SQL queries must utilize B-Tree indexes on `vendor`, `employee`, `category`, and `transaction_time`. |
| **NFR-REL-01** | Reliability | Multi-row operations must be atomic (`connection.setAutoCommit(false)` or `@Transactional`). |
| **NFR-REL-02** | Reliability | Integrity constraints: Cascade deletes from transactions to analysis runs and findings; Restrict deletes on audit decisions and events. |
| **NFR-MAINT-01**| Maintainability | Persistence layer must use plain JDBC (`JdbcTemplate`, `DataSource`) without JPA/Hibernate ORM complexity. |
| **NFR-COMP-01** | Compatibility | The backend must run on Java 17 LTS and the frontend must compile to static assets with Vite 8.x for Nginx hosting. |

---

## 5. Constraints, Dependencies & Assumptions

### 5.1 Technical Constraints
1. **No JPA/Hibernate**: By architectural decision, ORM frameworks are prohibited to ensure complete transparency over SQL execution.
2. **Stateless Backend**: The Spring Boot backend stores no HTTP session state; all authorization context is conveyed via JWT.
3. **Deterministic Logic**: Risk evaluation rules must not rely on non-deterministic external inputs (e.g. random number generators or external web APIs).

### 5.2 External Dependencies
- PostgreSQL 16 database server.
- Modern web browser with ECMAScript 2022+ and CSS Grid / Flexbox support.
- Docker engine for containerized orchestration.

### 5.3 Operating Assumptions
- Financial transactions imported into the platform are denominated in a single consistent currency (INR).
- The transaction timestamp format adheres to ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss`) or standard RFC 4180 timestamps.
- System clocks across application containers and database servers are synchronized via NTP.
