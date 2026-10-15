# AUREX — UML Class Diagram Specification

**Document Version:** 1.0  
**Project Name:** AUREX — Financial Audit Risk Platform  
**Academic Context:** BCA Field Project, MIT Arts, Commerce & Science College, Alandi  
**Diagram Source:** [`diagrams/class-diagram.puml`](diagrams/class-diagram.puml)  

---

## 1. Class Architecture & Design Patterns

The backend object model of AUREX is designed using clean architectural layering, strict separation of concerns, and classic object-oriented design patterns:
1. **Strategy Pattern (`rule` package)**: The deterministic risk engine defines `RiskRule` and `DatasetRiskRule` interfaces. Individual policies (`HighAmountRule`, `TransactionVelocityRule`, etc.) encapsulate discrete evaluation algorithms, allowing policies to be added or modified independently without changing the engine.
2. **Factory Pattern (`RiskEngineFactory`)**: Decouples the creation and configuration of the `RiskEngine` instance from its execution, injecting active thresholds from `RiskConfiguration`.
3. **Facade Pattern (`TransactionImportFacade`)**: Provides a Spring-managed service abstraction over the lower-level `TransactionImportService` and `TransactionCsvReader`.
4. **Data Access Object (DAO / Repository Pattern)**: Repositories encapsulate all raw SQL operations using plain JDBC (`PreparedStatement`, `JdbcTemplate`, `DataSourceUtils`), completely eliminating ORM impedance mismatch.
5. **Data Transfer Object (DTO) Pattern**: Request and response objects (`LoginRequest`, `CreateAuditDecisionRequest`, `UserDto`, etc.) prevent domain entity exposure and sanitize sensitive fields (such as password hashes).

---

## 2. Class Diagram Representation

<img width="1122" height="716" alt="Class_Diagram" src="https://github.com/user-attachments/assets/f2268ec4-8f59-41a2-8663-a6b59d2125f0" />



## 3. Package & Responsibility Inventory

### 3.1 Package: `rule`
Contains the core deterministic risk evaluation abstractions and implementations:
- **`RiskRule`**: Interface defining single-transaction evaluation: `RiskFinding evaluate(Transaction transaction)`. Implemented by `HighAmountRule`, `UnusualTimeRule`, `RoundAmountRule`.
- **`DatasetRiskRule`**: Interface defining cross-dataset evaluation: `RiskFinding evaluate(Transaction transaction, List<Transaction> transactions)`. Implemented by `TransactionVelocityRule`, `DuplicateTransactionRule`, `VendorConcentrationRule`.
- **`DuplicateTransactionIndex`**: In-memory hash index grouping transactions by composite key `(vendor, employee, amount, category)` to accelerate $O(1)$ duplicate lookups.

### 3.2 Package: `service` / `com.vighnesh.service`
Orchestrates business operations, database transactions, and component interactions:
- **`RiskEngine`**: Aggregates registered rules and executes evaluation against a transaction, passing findings to `RiskScoreCalculator`.
- **`RiskEngineFactory`**: Spring `@Component` that constructs fresh `RiskEngine` instances wired with active configuration properties.
- **`RiskScoreCalculator`**: Sums finding scores, applies the 100-point ceiling, and categorizes severity tiers (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
- **`RiskAnalysisService`**: Primary transactional orchestrator for single-transaction analysis, batch analysis, history run retrieval, and finding persistence.
- **`AuditService`**: Enforces business validation on audit decision recording, sources the actor from the authenticated `Principal`, and manages dual-table transactional logging.
- **`TransactionImportFacade` & `TransactionImportService`**: Manages file parsing and atomic plain JDBC batch insertion with rollback safety.
- **`AnalyticsService` & `RiskEvidenceService`**: Computes multi-dimensional risk metrics and cross-entity relationship context graphs.

### 3.3 Package: `repository`
Encapsulates data persistence using plain JDBC:
- **`TransactionRepository`**: Direct SQL queries (`save`, `findById`, `findAll`) on `transactions`.
- **`RiskAnalysisRunRepository`**: Manages insert, pagination, and latest-run queries on `risk_analysis_runs`.
- **`RiskFindingRepository`**: Manages batch insertion and query retrieval of findings by analysis run IDs.
- **`AuditDecisionRepository` & `AuditEventRepository`**: Manages transactional persistence and historical queries for formal compliance records.
- **`AnalyticsRepository`**: Executes advanced PostgreSQL aggregation queries (`DISTINCT ON`, `GROUP BY`, `COALESCE`) for dashboard intelligence.

### 3.4 Package: `com.vighnesh.user` & `com.vighnesh.auth`
Manages identity, authentication, and access control:
- **`UserRepository`**: Uses Spring's `JdbcTemplate` for user lookups, inserts with `GeneratedKeyHolder`, and pessimistic row locking (`findAdminsForUpdate`).
- **`UserService`**: Implements user creation with BCrypt encoding, account status toggling, self-disable protection, and last-admin lockout safety.
- **`AuthService`**: Coordinates password verification and JWT token issuance.
- **`JwtService`**: Handles HMAC-SHA256 signature generation, validation, and claim extraction.

### 3.5 Package: `com.vighnesh.controller`
Exposes the REST API layer:
- **`AuthController`**: Handles `/api/auth/login` and `/api/auth/me`.
- **`UserController`**: Handles `/api/users` endpoints guarded by `hasRole('ADMIN')`.
- **`TransactionController` & `TransactionImportController`**: Ingestion and raw transaction browsing.
- **`RiskController` & `RiskIntelligenceController`**: Risk reporting, batch evaluation, and executive intelligence.
- **`AuditController`**: Audit decision submission and event retrieval.
- **`HealthController`**: Database and application health check.
- **`GlobalExceptionHandler`**: `@RestControllerAdvice` mapping exceptions to standard JSON error responses.
