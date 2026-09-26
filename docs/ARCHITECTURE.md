# AUREX — System Architecture & Technical Design

This document details the architectural principles, system design, data flows, and technical rationale underlying the **AUREX Financial Audit Risk Intelligence Platform**.

---

## 🏛️ Core Design Principles

1. **Deterministic Risk Baseline**: Audit compliance demands reproducible, explainable, and verifiable risk scores. Machine learning or non-deterministic AI models can hallucinate or fluctuate, making them unsuitable as primary legal/audit scoring authorities. AUREX enforces 100% deterministic policy rules.
2. **High-Performance Light Stack**: By avoiding JPA/Hibernate ORM overhead, AUREX leverages **Plain Spring JDBC** (`JdbcTemplate` and direct SQL parameterization). This eliminates hidden lazy-loading N+1 queries, unexpected state mutations, and heavy entity caching layers.
3. **Persisted Audit Evolution**: Risk scores are snapshot-persisted per evaluation run (`risk_analysis_runs`), allowing forensic auditors to track how risk evaluations evolve over time as new data or policies enter the system.
4. **Graph-Oriented Evidence Context**: Single transaction anomalies rarely happen in isolation. AUREX calculates surrounding evidence across vendor, employee, and category dimensions in SQL, aggregating peak risk scores and concentration metrics without polluting primary entity models.

---

## 📐 High-Level System Architecture

```text
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           REACT / TYPESCRIPT FRONTEND                           │
│  (Dashboard | Transaction Explorer | Investigation Workspace | Intelligence)    │
└───────────────────────────────────────┬─────────────────────────────────────────┘
                                        │ HTTP REST / JSON
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            SPRING BOOT REST API LAYER                           │
│  (HealthController | RiskController | TransactionController | ImportController) │
└───────────────────────────────────────┬─────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                             BUSINESS & FACADE SERVICES                          │
│     (RiskAnalysisService | RiskEvidenceService | TransactionImportFacade)       │
└───────────────────┬─────────────────────────────────────────┬───────────────────┘
                    │                                         │
                    ▼                                         ▼
┌───────────────────────────────────────┐   ┌─────────────────────────────────────┐
│       DETERMINISTIC RISK ENGINE       │   │       PLAIN JDBC REPOSITORIES       │
│  (HighAmountRule | VelocityRule |     │   │  (TransactionRepository |           │
│   VendorConcentrationRule | etc.)     │   │   RiskEvidenceRepository | etc.)    │
└───────────────────────────────────────┘   └──────────────────┬──────────────────┘
                                                               │ SQL Queries
                                                               ▼
                                            ┌─────────────────────────────────────┐
                                            │         POSTGRESQL DATABASE         │
                                            │  (transactions, risk_analysis_runs, │
                                            │   risk_findings with B-Tree indexes)│
                                            └─────────────────────────────────────┘
```

---

## 🔬 Risk Engine Architecture & Policy Evaluation

The risk engine evaluates transactions against 6 independent, configurable audit rules:

1. **High Amount Rule**: Flags transactions exceeding a threshold (e.g. ₹100,000) with +30 risk points.
2. **Round Amount Rule**: Flags structured round-number payments (multiples of ₹10,000) with +10 risk points.
3. **Unusual Time Rule**: Flags transactions executed outside standard business hours (09:00 - 18:00) with +20 risk points.
4. **Duplicate Transaction Rule**: Flags identical amount and vendor payments within a 10-minute sliding window with +25 risk points.
5. **Transaction Velocity Rule**: Flags employees submitting more than 3 transactions within a 30-minute window with +20 risk points.
6. **Vendor Concentration Rule**: Flags vendor spending exceeding 70% of an employee's total transaction volume with +15 risk points.

### Score Aggregation & Risk Severity Bounds
```text
Total Score = Min(100, Sum(Triggered Policy Points))

Severity Classification:
  • LOW      :   0 <= Score < 30
  • MEDIUM   :  30 <= Score < 60
  • HIGH     :  60 <= Score < 80
  • CRITICAL :  80 <= Score <= 100
```

---

## 💾 Database Schema & Indexing Strategy

### Table Relationships
- `transactions` (1) ───< `risk_analysis_runs` (N)
- `risk_analysis_runs` (1) ───< `risk_findings` (N)

### Query Optimization & Indexing
To support real-time investigative workspace context queries, composite and single-column B-Tree indexes are defined:

- `transactions`: Indexes on `vendor`, `employee`, `category`, and `transaction_time` enable sub-millisecond filtering for evidence relationship grouping.
- `risk_analysis_runs`: Composite index `idx_analysis_runs_txn_analyzed (transaction_id, analyzed_at DESC, id DESC)` optimizes SQL `DISTINCT ON` queries retrieving the latest evaluation run per related transaction without full-table scans.

---

## 🧠 Key Design Decisions & Technical Trade-offs

### 1. Why Deterministic Rules Instead of ML / AI for Base Scoring?
- **Auditability & Legal Compliance**: Financial compliance requires exact audit trails. A court or regulator will reject a black-box AI score. Deterministic rules produce exact point additions with human-readable explanations.
- **Role of AI in AUREX**: AI is positioned as a **narrative assistant layer sitting on top of deterministic evidence graph data**, generating executive summaries without manipulating underlying risk metrics.

### 2. Why Plain JDBC Instead of Spring Data JPA / Hibernate?
- **Performance & Predictability**: Financial platforms perform heavy aggregation across thousands of transactions. ORM frameworks generate complex, unoptimized SQL joins and entity overhead. Plain JDBC provides explicit control over SQL queries, parameter binding, and execution plans.
- **Zero N+1 Query Risk**: Repository methods directly construct clean DTO payloads (`TransactionContext`, `RelatedGroup`) from single or CTE SQL queries.

### 3. Why Persisted Analysis History?
- **Audit Evolution Tracking**: Re-evaluating risk dynamically in memory would erase historical context. Persisting `risk_analysis_runs` records the precise historical state of compliance evaluations, allowing auditors to inspect previous runs when policies or transaction data change.

---

## ⚡ Frontend Architecture & Code-Splitting

- **Modular React Component Hierarchy**: Decoupled presentation components (`RiskBadge`, `RiskScoreGauge`, `FindingCard`) from container page views.
- **Route-Level Code Splitting**: Utilizing `React.lazy` and `React.Suspense` to split bundle chunks per route (`/transactions`, `/intelligence`, `/investigations`), ensuring initial page loads remain lightweight.
- **Central API Abstraction**: `apiClient` manages base URL resolution via `VITE_API_BASE_URL` with interceptors standardizing client-side error handling.
