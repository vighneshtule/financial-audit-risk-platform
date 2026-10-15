# AUREX — Project Submission Master Documentation Index

| Academic & Project Metadata | Details |
| :--- | :--- |
| **Project Title** | AUREX — Financial Audit Risk Platform |
| **Degree Program** | Bachelor of Computer Applications (BCA) — Field Project |
| **Institution** | MIT Arts, Commerce & Science College, Alandi (Pune) |
| **Academic Year** | 2025–2026 |
| **Repository URL** | [https://github.com/vighneshtule/financial-audit-risk-platform](https://github.com/vighneshtule/financial-audit-risk-platform) |
| **Local Workspace** | `E:\Vighnesh\financial-audit-risk-platform` |
| **Document Version** | 1.0.1 (Reviewed & Corrected) |
| **Source of Truth** | Verified from real code, schema, tests, and configuration |

---

## 1. Executive Guide for Evaluators & Professors

This documentation pack has been compiled following rigorous academic and industry software engineering standards. Every claim, schema definition, class hierarchy, risk threshold, and test result is directly traceable to the active codebase. 

- **No Synthetic or Fabricated Claims**: No artificial customer interviews, imaginary metrics, or unbuilt machine learning features are presented as implemented. All future roadmap items are strictly segregated.
- **Strict Implementation Truth**: Validated against Java 17, Spring Boot 3.5.6, Plain JDBC (zero JPA/Hibernate), PostgreSQL 16, React 19, TypeScript, Tailwind CSS, Docker Compose, and Nginx.
- **Dual-Level Verification**: Verified via **175 automated backend integration tests** running against real Testcontainers PostgreSQL 16 (0 failures) and **56 frontend automated unit/component tests** (0 failures).

---

## 2. Complete Document Catalog

All documentation artifacts are stored under `docs/project-submission/`:

| # | Document File | Description & Core Contents | Academic Relevance |
| :-: | :--- | :--- | :--- |
| 1 | [REPOSITORY_FINDINGS.md](REPOSITORY_FINDINGS.md) | Exhaustive audit of the active repository, verified technology versions, class inventory, database tables, complete API endpoint list, exact risk thresholds, test run outputs, and documented limitations. | System Discovery & Technical Audit |
| 2 | [PRD.md](PRD.md) | Product Requirements Document including executive summary, problem statement, functional requirements (`FR-AUTH-01` to `FR-METRIC-03`), non-functional requirements, user stories with acceptance criteria, and explicit in-scope vs. roadmap boundaries. | Requirements Engineering & Business Analysis |
| 3 | [SYSTEM_REQUIREMENTS.md](SYSTEM_REQUIREMENTS.md) | Software Requirements Specification (SRS) detailing minimum hardware specifications, software prerequisites, development environment, runtime deployment constraints, and testable requirement matrices. | Software Engineering (SRS) |
| 4 | [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md) | Logical 6-tier architecture vs. Physical Docker Compose deployment architecture, Nginx reverse proxy routing, security boundaries, plain JDBC data access rationale, and container health check topologies. | System Design & Enterprise Architecture |
| 5 | [API_DOCUMENTATION.md](API_DOCUMENTATION.md) | Comprehensive REST API reference documenting all controller endpoints across 6 groups, including HTTP methods, authentication requirements, role permissions, request/response JSON schemas, and HTTP status codes. | API Engineering & Integration |
| 6 | [DATA_DICTIONARY.md](DATA_DICTIONARY.md) | Database data dictionary for all 6 tables (`users`, `transactions`, `risk_analysis_runs`, `risk_findings`, `audit_decisions`, `audit_events`), documenting column types, nullability, primary/foreign keys, default values, check constraints, and B-Tree indexes. | Database Design & Data Modeling |
| 7 | [USER_ROLES_AND_FLOWS.md](USER_ROLES_AND_FLOWS.md) | Detailed analysis of the 3 user roles (`ADMIN`, `AUDITOR`, `VIEWER`), comprehensive role-permission matrix, user journey diagram, transaction review workflows, account deactivation mechanisms, and self-lockout prevention. | Security Analysis & UX Flows |
| 8 | [DFD.md](DFD.md) | Structured Data Flow Diagrams including Level 0 Context Diagram (external actors and system boundary) and Level 1 Functional Decomposition Diagram (Processes 1.0 to 6.0 and Data Stores D1 to D6) using Gane-Sarson / Yourdon conventions. | Structured Systems Analysis |
| 9 | [ER_DIAGRAM.md](ER_DIAGRAM.md) | Entity-Relationship documentation detailing table schemas, Crow's Foot cardinality notations, enforced foreign keys vs. application-managed relationships, and index optimization strategies. | Relational Database Modeling |
| 10 | [CLASS_DIAGRAM.md](CLASS_DIAGRAM.md) | Object-oriented UML class diagram representing domain models, Plain JDBC repositories, service interfaces, controller endpoints, DTO transfer contracts, and the Strategy pattern rule engine. | Object-Oriented Analysis & Design (OOAD) |
| 11 | [OBJECT_DIAGRAM.md](OBJECT_DIAGRAM.md) | UML object diagram illustrating a concrete illustrative runtime state snapshot for transaction `TXN-90214`, risk run `1042`, two triggered findings, audit decision `501`, and audit event `901`. All values are illustrative examples, not production records. Includes theoretical class vs. object comparison. | UML Runtime Analysis |
| 12 | [SEQUENCE_DIAGRAMS.md](SEQUENCE_DIAGRAMS.md) | Detailed UML sequence diagrams for 5 primary workflows: (1) JWT Login & Status Check, (2) Deterministic Risk Evaluation, (3) Audit Decision Dual-Write (Spring `@Transactional`), (4) RFC 4180 CSV Batch Ingestion, and (5) Administrative User Provisioning. | Dynamic Behavioral Modeling |
| 13 | [USE_CASE_DIAGRAM.md](USE_CASE_DIAGRAM.md) | UML use-case diagram defining primary actors and use-cases (`UC-01` to `UC-07`), formal `<<include>>` and `<<extend>>` relationships, and full tabular specifications (preconditions, flows, postconditions). | Functional Requirements & Use-Case Modeling |
| 14 | [TEST_PLAN_AND_RESULTS.md](TEST_PLAN_AND_RESULTS.md) | Comprehensive QA test plan and verified execution results: 175 backend Testcontainers tests (100% pass in 4m 39s), 56 frontend tests (100% pass in 315ms), manual viva demonstration test cases, and pass/fail criteria. | Quality Assurance & Software Testing |
| 15 | [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md) | Bidirectional Requirements Traceability Matrix (RTM) linking all 35 Functional Requirements (`FR-AUTH-01` to `FR-METRIC-03`) and 12 Non-Functional Requirements to concrete source code classes, API endpoints, test cases, and verification status. | Project Management & Compliance |
| 16 | [GLOSSARY.md](GLOSSARY.md) | Technical and financial domain terminology dictionary defining 25+ core concepts (Plain JDBC, RFC 4180, Deterministic Risk Rule, Dual-Write, Vendor Concentration, BCrypt, Testcontainers, etc.). | Viva Preparation & Terminology Reference |
| 17 | [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md) | This master navigation index linking all documents, diagram files, architectural summaries, and viva guidance. | Project Submission Master Index |
| 18 | [FINAL_DOCUMENTATION_REVIEW.md](FINAL_DOCUMENTATION_REVIEW.md) | Final pre-submission documentation review record: all corrections made, methodology, `git diff --check` output, unresolved uncertainties, and a checklist of all checks performed. | Quality Gate & Submission Audit Trail |

---

## 3. Diagram Catalog (`docs/project-submission/diagrams/`)

The platform contains 14 presentation-ready diagram source files written in standard Mermaid (`.mmd`) and PlantUML (`.puml`):

| Diagram Source File | Format | Diagram Type & Description | Embedded In |
| :--- | :---: | :--- | :--- |
| [`dfd-context.mmd`](diagrams/dfd-context.mmd) | Mermaid | DFD Level 0 Context Diagram showing external actors and AUREX system boundary | [DFD.md](DFD.md) |
| [`dfd-level-1.mmd`](diagrams/dfd-level-1.mmd) | Mermaid | DFD Level 1 Process Decomposition (Processes 1.0–6.0, Stores D1–D6) | [DFD.md](DFD.md) |
| [`er-diagram.mmd`](diagrams/er-diagram.mmd) | Mermaid | Entity-Relationship Diagram with all 6 PostgreSQL tables and cardinalities | [ER_DIAGRAM.md](ER_DIAGRAM.md) |
| [`class-diagram.puml`](diagrams/class-diagram.puml) | PlantUML | Comprehensive UML Class Diagram covering all Spring Boot packages and patterns | [CLASS_DIAGRAM.md](CLASS_DIAGRAM.md) |
| [`object-diagram.puml`](diagrams/object-diagram.puml) | PlantUML | UML Object Diagram of illustrative runtime snapshot (Transaction `TXN-90214`, Run `1042`, Findings) | [OBJECT_DIAGRAM.md](OBJECT_DIAGRAM.md) |
| [`user-roles.mmd`](diagrams/user-roles.mmd) | Mermaid | Role-Permission hierarchy and operational boundary diagram | [USER_ROLES_AND_FLOWS.md](USER_ROLES_AND_FLOWS.md) |
| [`user-journey.mmd`](diagrams/user-journey.mmd) | Mermaid | End-to-end user journey across authentication, ingestion, analysis, and auditing | [USER_ROLES_AND_FLOWS.md](USER_ROLES_AND_FLOWS.md) |
| [`sequence-login.mmd`](diagrams/sequence-login.mmd) | Mermaid | Sequence: User login, BCrypt verification, JWT issuance, per-request status check | [SEQUENCE_DIAGRAMS.md](SEQUENCE_DIAGRAMS.md) |
| [`sequence-risk-analysis.mmd`](diagrams/sequence-risk-analysis.mmd) | Mermaid | Sequence: In-memory deterministic rule execution, score capping, persistence | [SEQUENCE_DIAGRAMS.md](SEQUENCE_DIAGRAMS.md) |
| [`sequence-audit-decision.mmd`](diagrams/sequence-audit-decision.mmd) | Mermaid | Sequence: Audit decision recording, principal extraction, Spring `@Transactional` dual-write | [SEQUENCE_DIAGRAMS.md](SEQUENCE_DIAGRAMS.md) |
| [`sequence-csv-import.mmd`](diagrams/sequence-csv-import.mmd) | Mermaid | Sequence: RFC 4180 CSV batch parsing, validation, and plain JDBC batch insertion | [SEQUENCE_DIAGRAMS.md](SEQUENCE_DIAGRAMS.md) |
| [`use-case.puml`](diagrams/use-case.puml) | PlantUML | UML Use Case Diagram showing actors (`Admin`, `Auditor`, `Viewer`) and relationships | [USE_CASE_DIAGRAM.md](USE_CASE_DIAGRAM.md) |
| [`logical-architecture.mmd`](diagrams/logical-architecture.mmd) | Mermaid | 6-tier logical architecture diagram (Presentation to Database layer) | [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md) |
| [`deployment-architecture.mmd`](diagrams/deployment-architecture.mmd) | Mermaid | Containerized deployment topology (Nginx, Spring Boot, PostgreSQL, volumes, network) | [SYSTEM_ARCHITECTURE.md](SYSTEM_ARCHITECTURE.md) |

> **Note on Diagram Rendering**:
> The diagram source files are saved in standard, portable formats compatible with GitHub, VS Code Mermaid/PlantUML extensions, and online live editors ([mermaid.live](https://mermaid.live) and [plantuml.com](http://www.plantuml.com/plantuml)). Local CLI rendering utilities (`mmdc`, `plantuml.jar`) were not bundled in the Windows PATH, so pure text source files were generated without synthetic image artifacts.

---

## 4. Key Academic Viva Demonstration Guide

When presenting AUREX to examiners and college professors, highlight the following standout architectural decisions:

1. **Why Plain JDBC instead of Hibernate/JPA?**
   - *Academic justification*: Financial audit platforms require absolute determinism, fine-grained query control, and zero hidden state. Plain JDBC with Spring `JdbcTemplate` prevents the infamous N+1 select problem, eliminates unmanaged entity cache side effects, minimises memory footprints during high-volume batch processing, and ensures that exact SQL queries are completely auditable.
2. **How does the Deterministic Risk Engine function?**
   - *Academic justification*: Rather than black-box probabilistic heuristics, AUREX uses 6 deterministic rules implementing a shared `RiskRule` interface (Strategy Design Pattern). Each rule evaluates specific forensic accounting criteria (velocity, duplicates, concentration, round numbers, unusual timing, and materiality). Scores are aggregated and capped at 100, and the stored `explanation` field makes every finding transparent.
3. **How is Compliance and Non-Repudiation Supported?**
   - *Academic justification*: Through an atomic dual-write pattern within a Spring `@Transactional` boundary. `audit_decisions` records the operational state; `audit_events` provides a chronological, application-level append-only log (no UPDATE/DELETE APIs). Auditor identity is automatically bound from the authenticated JWT principal (`Principal.getName()`), preventing clients from claiming a different actor in the request body. Note: this is application-level enforcement; the system does not use cryptographic signing or database-level immutability (WORM).
4. **How does Authentication & Revocation work?**
   - *Academic justification*: Stateless HMAC-SHA256 JWTs combined with real-time per-request database validation in `JwtAuthenticationFilter`. When an administrator deactivates a compromised account, the user is immediately rejected on their very next HTTP request without waiting for the token to expire.

---

*AUREX — Developed for Bachelor of Computer Applications (BCA) Field Project Evaluation, MIT Arts, Commerce & Science College, Alandi.*
