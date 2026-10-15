# AUREX — Final Documentation Review

| Metadata | Details |
| :--- | :--- |
| **Review Date** | October 2026 |
| **Reviewer** | Documentation Consistency & Academic Accuracy Review |
| **Scope** | `docs/project-submission/` and `docs/ARCHITECTURE.md` |
| **Constraint** | Documentation files only — no source code, configuration, secrets, Docker volumes, or Git history modified |

---

## 1. Review Methodology

The following source artefacts were inspected directly before making any documentation edits:

| Artefact | Purpose |
| :--- | :--- |
| `src/main/resources/schema.sql` | Ground truth for tables, columns, FK constraints, indexes |
| `docker-compose.yml` | Ground truth for container names, published ports, healthchecks, volumes |
| `frontend/nginx.conf` | Nginx routing, proxy targets, gzip, upload limit |
| `src/main/resources/application.properties` | Spring Boot profile, datasource, upload size |
| `src/main/java/com/vighnesh/service/AuditService.java` | Dual-write `@Transactional` pattern |
| `src/main/java/repository/AuditEventRepository.java` | Append-only repository (no UPDATE/DELETE) |

---

## 2. Corrections Made

### 2.1 Absolute `file:///` Links → Relative Links

All machine-absolute `file:///E:/Vighnesh/...` URLs replaced with portable relative Markdown links in every document that contained them. Affected files:

| File | Previous | Fixed To |
| :--- | :--- | :--- |
| `ER_DIAGRAM.md` | `file:///E:/...` diagram links | `diagrams/er-diagram.mmd` |
| `DATA_DICTIONARY.md` | `file:///E:/...` links | Relative sibling links |
| `DFD.md` | `file:///E:/...` links | `diagrams/dfd-*.mmd` |
| `SEQUENCE_DIAGRAMS.md` | `file:///E:/...` links | `diagrams/sequence-*.mmd` |
| `CLASS_DIAGRAM.md` | `file:///E:/...` link | `diagrams/class-diagram.puml` |
| `OBJECT_DIAGRAM.md` | `file:///E:/...` link | `diagrams/object-diagram.puml` |
| `USE_CASE_DIAGRAM.md` | `file:///E:/...` link | `diagrams/use-case.puml` |
| `USER_ROLES_AND_FLOWS.md` | `file:///E:/...` links | `diagrams/user-*.mmd` |
| `SYSTEM_ARCHITECTURE.md` | `file:///E:/...` links | `diagrams/logical-architecture.mmd`, `diagrams/deployment-architecture.mmd` |
| `REQUIREMENTS_TRACEABILITY.md` | `file:///e:/...` links | Relative sibling `.md` links |
| `TEST_PLAN_AND_RESULTS.md` | `file:///E:/...` test file links | `../../src/test/...` and `../../frontend/src/...` relative paths |
| `REPOSITORY_FINDINGS.md` | `file:///E:/...` schema link | `../../src/main/resources/schema.sql` |
| `DOCUMENTATION_INDEX.md` | All 17 doc links + 14 diagram links used `file:///` | All replaced with relative links; file rewritten |

### 2.2 Deployment Port Exposure Accuracy

Compared all port descriptions against `docker-compose.yml`:

| Container | Finding | Fix Applied |
| :--- | :--- | :--- |
| `aurex-frontend` | Published host port is `${FRONTEND_PORT:-80}` (only externally reachable container) | Documented clearly as host-published |
| `aurex-backend` | Port 8080 is internal to `aurex-network` only; NOT published to host | Corrected from vague "exposes port 8080" to "internal, not published to host" |
| `aurex-postgres` | Port 5432 is restricted to `aurex-network`; the `127.0.0.1:5432:5432` line is commented-out | Added note about commented debug option |

Updated files: `diagrams/deployment-architecture.mmd`, `SYSTEM_ARCHITECTURE.md`, `REPOSITORY_FINDINGS.md`.

### 2.3 Database FK and Column Accuracy

Compared `ER_DIAGRAM.md`, `DATA_DICTIONARY.md`, `DFD.md`, and `diagrams/er-diagram.mmd` against `schema.sql`:

| Issue | Finding | Fix Applied |
| :--- | :--- | :--- |
| `risk_findings.run_id` referenced in docs | Column does not exist in SQL. Actual column is `analysis_run_id`. FK: `risk_findings.analysis_run_id → risk_analysis_runs(id)` ON DELETE CASCADE | Corrected in ER_DIAGRAM.md, DATA_DICTIONARY.md, DFD.md |
| Missing FK: `audit_events.decision_id → audit_decisions(id)` | Was absent or understated in older docs | Added explicitly with ON DELETE RESTRICT |
| Missing FK: `audit_events.transaction_id → transactions(transaction_id)` | Was absent in some doc sections | Added explicitly with ON DELETE RESTRICT |
| `risk_findings.transaction_id → transactions(transaction_id)` | Was not clearly documented | Added with ON DELETE CASCADE |

### 2.4 Audit Trail Immutability — Terminology Corrected

The original documentation repeatedly used language like "immutable audit events", "cryptographically tamper-proof", and "non-repudiation" without evidence.

**What the code actually provides:**
- `AuditEventRepository` has INSERT and SELECT operations only — no UPDATE or DELETE methods.
- `AuditService.recordDecision()` is annotated `@Transactional`, guaranteeing atomicity of the decision + event dual-write.
- There is no database-level WORM constraint, trigger, or row-level security preventing direct SQL deletion.
- There is no cryptographic signing of audit records.

**Corrected language applied throughout:** "append-only at the application layer", "application-enforced", "no UPDATE/DELETE APIs exposed". Affected files:

`PRD.md`, `DATA_DICTIONARY.md`, `ER_DIAGRAM.md`, `SEQUENCE_DIAGRAMS.md`, `SYSTEM_ARCHITECTURE.md`, `REQUIREMENTS_TRACEABILITY.md`, `API_DOCUMENTATION.md`, `GLOSSARY.md`, `REPOSITORY_FINDINGS.md`, `DOCUMENTATION_INDEX.md`.

The `GLOSSARY.md` heading **"Immutable Event Ledger"** was renamed to **"Append-Only Event Log"** with an explicit note distinguishing it from WORM/cryptographic tamper evidence.

### 2.5 Unverified Performance Claims Removed or Qualified

| Claim | Location | Action |
| :--- | :--- | :--- |
| "sub-millisecond filtering" | `docs/ARCHITECTURE.md` line 86 | Replaced with "support fast filtering (no formal benchmark has been performed)" |
| "Sub-100ms single transaction analysis execution" — marked VERIFIED | `REQUIREMENTS_TRACEABILITY.md` NFR-PERF-01 | Reclassified to `PARTIALLY VERIFIED`; evidence note updated to clarify suite timing ≠ per-request benchmark |
| Sub-millisecond query claim | `DATA_DICTIONARY.md` | Removed in prior session |

### 2.6 Object Diagram Illustrative Label

`OBJECT_DIAGRAM.md` and `DOCUMENTATION_INDEX.md` now explicitly state that all runtime values in the object diagram are **illustrative examples constructed to demonstrate object types and attribute bindings**, not production records.

### 2.7 Source Code Package Name Correction

`REQUIREMENTS_TRACEABILITY.md` referenced `com.aurex.*` as the Java package. Actual package is `com.vighnesh.*`. Corrected. Frontend path also corrected from `src/*` to `frontend/src/*`.

---

## 3. `git diff --check` Output

```
(exit code 0 — no trailing whitespace errors detected)
```

---

## 4. Files Changed (Git Status)

| Status | File |
| :--- | :--- |
| Modified (tracked) | `docs/ARCHITECTURE.md` |
| New (untracked) | `docs/project-submission/` *(entire directory — new submission pack)* |

All changes are documentation-only. No application source code, SQL schema, configuration files, secrets, Docker volumes, or Git branches were touched.

---

## 5. Unresolved Uncertainties

| # | Uncertainty | Notes |
| :--- | :--- | :--- |
| 1 | **Per-request latency benchmark (NFR-PERF-01)** | The 175-test suite runs in 4m 39s but this is suite-level timing, not per-transaction analysis timing. No isolated JMH or k6 benchmark was executed. Requirement marked `PARTIALLY VERIFIED`. |
| 2 | **`risk_findings.analysis_run_id` — application mapping** | The FK `analysis_run_id → risk_analysis_runs(id)` is enforced by the database. However, `risk_findings` also has `transaction_id` as a redundant FK. The rationale for this redundancy is not documented in comments. It is assumed intentional (denormalization for faster per-transaction queries). |
| 3 | **Diagram rendering** | `mmdc` (Mermaid CLI) and `plantuml.jar` are not installed in the Windows PATH. All `.mmd` and `.puml` files are syntactically authored but were not rendered to PNG/SVG for this review. Rendering must be performed via GitHub native Mermaid preview, VS Code extensions, or online editors. |
| 4 | **`docs/DEPLOYMENT.md` not reviewed** | `docs/DEPLOYMENT.md` exists alongside `docs/ARCHITECTURE.md` but was not part of the `docs/project-submission/` scope. It should be checked for any stale port or immutability claims before final submission if it is included in the submission pack. |
| 5 | **Test class location discrepancy** | `TEST_PLAN_AND_RESULTS.md` section 3.3 references `RiskControllerIntegrationTest` without a `file:///` link. The actual test class package path was not independently confirmed. |

---

## 6. Checks Performed

- [x] All `file:///E:/...` and `file:///e:/...` links in `docs/project-submission/` removed and replaced with relative links
- [x] `docs/ARCHITECTURE.md` checked and "sub-millisecond" claim qualified
- [x] `docker-compose.yml` inspected; frontend, backend, postgres port exposure documented accurately
- [x] `schema.sql` FK constraints cross-checked against ER_DIAGRAM.md, DATA_DICTIONARY.md, DFD.md, er-diagram.mmd
- [x] `risk_findings.run_id` verified as non-existent; `analysis_run_id` confirmed as correct column
- [x] `audit_events` append-only enforcement confirmed via `AuditEventRepository.java`
- [x] `AuditService.java` `@Transactional` dual-write confirmed
- [x] No cryptographic signing or WORM enforcement found in codebase — all "immutable" language qualified
- [x] `NFR-PERF-01` (sub-100ms) reclassified to `PARTIALLY VERIFIED` in RTM
- [x] Object diagram labeled explicitly as illustrative
- [x] `git diff --check` run — exit code 0, no whitespace issues
- [x] No source code, configuration, secrets, Docker volumes, or Git branches modified
