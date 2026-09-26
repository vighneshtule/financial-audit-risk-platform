# AUREX — Financial Audit Risk Intelligence Platform

AUREX is an enterprise-grade financial audit risk platform designed to automatically evaluate corporate financial transactions against deterministic audit policies, detect risk velocity anomalies, track historical evaluation runs, and provide investigative evidence context across vendor, employee, and category dimensions.

---

## 🎯 Problem Statement
Corporate financial auditing traditionally relies on manual sample checking or disconnected spreadsheets, leading to undetected policy breaches, duplicate vendor payments, off-hours transaction anomalies, and unmonitored employee spending concentration. AUREX solves this by ingesting transaction streams and evaluating them deterministically against multi-dimensional audit rules with persisted historical audit trails.

---

## ✨ Features
- **Transaction Ingestion**: Multi-format CSV transaction import with validation and standard mapping.
- **Deterministic Risk Engine**: Multi-policy risk evaluation engine computing risk scores (0–100) and severity levels (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
- **Policy Violations & Findings**: Detailed rule breakdown including High Amount, Round Amount, Off-Hours Time, Transaction Velocity, Duplicate Transactions, and Vendor Concentration.
- **Transaction Investigation Workspace**: Dedicated workspace for forensic analysis featuring score composition, meta profiles, and historical run comparison.
- **Evidence & Relationship Intelligence**: Cross-entity graph context showing related vendor, employee, and category transactions with aggregate transaction volumes and peak risk scores.
- **Risk Intelligence Analytics**: Aggregate executive summary metrics, risk distribution, vendor/employee concentration metrics, and category risk profiling.
- **Audit Run Persistence & History**: Full audit trail recording historical analysis runs per transaction to track risk evolution over time.

---

## 🏗️ Architecture Overview

```text
  React + TypeScript SPA (Vite, Tailwind CSS, Recharts)
                            │
                            ▼ (REST API / Axios)
            Spring Boot 3.5.6 Web Application
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
    Deterministic Risk Engine      Services & Facades
    (6 Independent Rules)         (Import & Context)
              │                           │
              └─────────────┬─────────────┘
                            ▼ (Plain JDBC Connection Pool)
             PostgreSQL 16 Database Storage
```

---

## 🛠️ Technology Stack
- **Backend**: Java 17, Spring Boot 3.5.6, Plain JDBC (`JdbcTemplate` & `DataSource`, No JPA/Hibernate)
- **Database**: PostgreSQL 16 (Idempotent `schema.sql` with optimized B-Tree indexes)
- **API Documentation**: OpenAPI 3.0 / Springdoc Swagger UI
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, Framer Motion, Recharts, Lucide Icons
- **Testing**: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), MockMvc
- **Containerization & CI**: Docker, Docker Compose, GitHub Actions

---

## 🚀 Local Development Setup

### Prerequisites
- JDK 17+
- Node.js 20+ & npm
- PostgreSQL 16 (or Docker Desktop)

### 1. Database Setup
Ensure PostgreSQL is running locally on port 5432 and create the target database:
```sql
CREATE DATABASE financial_audit;
```

### 2. Backend Configuration & Startup
Copy `.env.example` to set environment variables or rely on local defaults:
```bash
# Run backend with Maven
mvn spring-boot:run
```
The backend API will be available at `http://localhost:8080`.
Swagger API documentation is available at `http://localhost:8080/swagger-ui.html`.

### 3. Frontend Setup & Startup
```bash
cd frontend
npm install
npm run dev
```
The frontend application will start at `http://localhost:5173`.

---

## 🐳 Docker Deployment

To launch the full stack (Spring Boot Backend + PostgreSQL Database) using Docker Compose:
```bash
docker-compose up --build
```

---

## 🧪 Testing

### Backend Unit & Integration Tests
```bash
mvn clean test
```

### Frontend Typecheck & Build Verification
```bash
cd frontend
npm run build
```

---

## 🔑 Environment Variables Reference

| Variable | Default Value | Description |
|---|---|---|
| `PORT` | `8080` | Spring Boot HTTP Server Port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/financial_audit` | PostgreSQL JDBC Connection String |
| `DB_USERNAME` | `postgres` | Database Username |
| `DB_PASSWORD` | `postgres` | Database Password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Allowed CORS Frontend Origins |
| `SPRING_PROFILES_ACTIVE` | `default` | Active Profile (`default` or `prod`) |
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Base API URL for Frontend |

---

## 📡 API Overview

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | System and Database Health Check |
| `GET` | `/api/transactions` | Fetch all ingested transactions |
| `POST` | `/api/transactions/import` | Upload & import CSV transactions file |
| `GET` | `/api/risk/summary` | Fetch executive risk overview metrics |
| `GET` | `/api/risk/transactions` | Paginated transaction risk list with level/score filters |
| `GET` | `/api/risk/transactions/{id}` | Live risk report for a specific transaction |
| `GET` | `/api/risk/transactions/{id}/context` | Related vendor, employee & category evidence context |
| `GET` | `/api/risk/transactions/{id}/history` | Analysis history runs for a transaction |
| `POST` | `/api/risk/analyze/{id}` | Evaluate & persist risk run for transaction |
| `POST` | `/api/risk/analyze-all` | Batch evaluate & persist risk runs across dataset |
| `GET` | `/api/risk/intelligence/summary` | Risk Intelligence multi-dimension analytics |

---

## 📂 Project Structure

```text
financial-audit-risk-platform/
├── .github/workflows/ci.yml       # GitHub Actions CI Workflow
├── Dockerfile                     # Multi-stage Dockerfile for Backend
├── docker-compose.yml             # Docker Compose orchestration
├── .env.example                   # Environment configuration template
├── pom.xml                        # Maven project descriptor
├── docs/                          # Architecture & Deployment Documentation
│   ├── ARCHITECTURE.md
│   └── DEPLOYMENT.md
├── src/main/java/
│   ├── com/vighnesh/
│   │   ├── config/                # Web, CORS & OpenAPI Configuration
│   │   ├── controller/            # REST API Controllers
│   │   ├── exception/             # Global Exception Handler
│   │   └── service/               # Business Services & Facades
│   ├── config/                    # Core JDBC Database Config
│   ├── model/                     # Domain & DTO Models
│   ├── repository/                # Plain JDBC Repositories
│   └── rule/                      # Deterministic Risk Policy Implementations
└── frontend/
    ├── src/
    │   ├── api/                   # Centralized Axios Client & API Endpoints
    │   ├── components/            # UI Components & Gauges
    │   ├── pages/                 # Application Page Views
    │   └── types/                 # TypeScript Type Definitions
    └── vite.config.ts             # Vite Build Configuration
```

---

## 🛣️ Product Roadmap
- [ ] **AI-Assisted Investigation Assistant**: Generative LLM layer sitting on top of deterministic evidence graph to provide automated narrative audit summaries.
- [ ] **Enterprise RBAC & Authentication**: JWT/OAuth2 role-based access control for compliance managers and auditors.
- [ ] **Case Management Workflow**: Flagged transaction lifecycle state management (`OPEN`, `IN_REVIEW`, `RESOLVED`, `ESCALATED`).
