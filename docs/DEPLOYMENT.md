# AUREX — Deployment & Operations Guide

This guide provides instructions for deploying and operating the **AUREX Financial Audit Risk Intelligence Platform** in production and containerized environments.

---

## 📋 Production Prerequisites

- **Host Environment**: Linux VM (Ubuntu 22.04 LTS recommended), AWS EC2, or equivalent cloud VM.
- **Container Runtime**: Docker 24+ & Docker Compose v2+.
- **Hardware Sizing**:
  - Minimum: 2 vCPU, 4 GB RAM, 20 GB SSD storage.
  - Recommended: 4 vCPU, 8 GB RAM, 50 GB SSD storage.
- **Network Ports**:
  - `80` (HTTP) / `443` (HTTPS when TLS reverse proxy is added).
  - PostgreSQL port `5432` and Spring Boot port `8080` are kept on internal Docker networks and are **not** exposed to the public internet.

---

## 🔑 1. Environment Secrets & Configuration

Create a local `.env` file on your server (from `.env.example`). Never commit this file.

```bash
cp .env.example .env
```

Generate secure random secrets:
```bash
# Generate 64-character base64 secret for JWT_SECRET
openssl rand -base64 64

# Generate strong database password
openssl rand -base64 24
```

Set the following in `.env`:
```env
FRONTEND_PORT=80
POSTGRES_DB=financial_audit
DB_USERNAME=postgres
DB_PASSWORD=<paste-your-generated-db-password>
JWT_SECRET=<paste-your-generated-jwt-secret>
SPRING_PROFILES_ACTIVE=prod
ENABLE_SWAGGER=false

# CORS: Set to your public domain(s) or http://localhost for local evaluation
CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:80

# First-Run Initial Admin Provisioning (see Section 3)
AUREX_PROD_INITIAL_ADMIN_PASSWORD=<temporary-strong-admin-password-min-12-chars>
AUREX_PROD_INITIAL_ADMIN_USERNAME=admin
```

---

## 🗄️ 2. Database Initialization & Persistence

- **Fresh Environment Bootstrap**:
  - The `postgres` service in `docker-compose.yml` mounts `./src/main/resources/schema.sql` into `/docker-entrypoint-initdb.d/schema.sql:ro`.
  - PostgreSQL executes this script automatically on the **first boot** of an empty database volume.
  - In production (`SPRING_PROFILES_ACTIVE=prod`), Spring Boot sets `spring.sql.init.mode=never`, preventing repetitive DDL lock acquisition and accidental migrations on subsequent container restarts.
- **Existing Database Volume Safety**:
  - Data is stored in the Docker volume `postgres_data` (`/var/lib/postgresql/data`).
  - Standard restarts (`docker compose down` and `docker compose up`) preserve all user accounts, transactions, and audit trails.
  - **Caution**: Never run `docker compose down -v` in a production environment as `-v` destroys persistent volumes.
- **Incremental Schema Changes**:
  - Apply incremental schema migrations (e.g. `ALTER TABLE`, new indexes) via `psql` directly into the database container before deploying new backend code:
    ```bash
    docker compose exec -T postgres psql -U postgres -d financial_audit -f migration.sql
    ```

---

## 👤 3. Initial Administrator Provisioning

In a fresh production database, the `users` table starts completely empty. To provision the initial administrator safely:

1. In `.env`, set `AUREX_PROD_INITIAL_ADMIN_PASSWORD` to a strong password (minimum 12 characters).
2. Start the stack:
   ```bash
   docker compose up -d --build
   ```
3. Verify the administrator was created:
   ```bash
   docker compose logs backend | grep "Production bootstrap"
   ```
4. Log into the platform at `http://localhost/` (or your domain) using `admin` and your supplied password.
5. **Security Hygiene**: Remove or blank out `AUREX_PROD_INITIAL_ADMIN_PASSWORD` in your `.env` file so the secret does not remain stored in configuration:
   ```bash
   sed -i '/AUREX_PROD_INITIAL_ADMIN_PASSWORD=/d' .env
   ```
   *Note: Subsequent restarts will safely skip admin bootstrap because existing accounts are detected.*

---

## 🐳 4. Starting and Managing the Full Stack

Launch all services (Nginx Frontend SPA, Spring Boot API, and PostgreSQL):
```bash
docker compose up -d --build
```

View status and health:
```bash
docker compose ps
```
All services (`aurex-postgres`, `aurex-backend`, `aurex-frontend`) should report `healthy` or `running`.

View logs:
```bash
docker compose logs -f backend
docker compose logs -f frontend
```

Graceful shutdown:
```bash
docker compose down
```

---

## 🌐 5. Architecture & Reverse Proxy Routing

```text
Public Internet (Port 80/443)
              │
              ▼
   ┌────────────────────────────────────────────────────────┐
   │ aurex-frontend (Nginx Alpine)                          │
   │                                                        │
   │  /        ──► Static SPA (/usr/share/nginx/html)       │
   │               (React Router fallback to index.html)    │
   │                                                        │
   │  /api/    ──► Proxy pass to http://backend:8080/api/   │
   └──────────────────────────┬─────────────────────────────┘
                              │ Internal Docker Network
                              ▼
   ┌────────────────────────────────────────────────────────┐
   │ aurex-backend (Spring Boot 3.5.6 on JRE 17)            │
   │  - Port 8080 internal only                             │
   │  - Health probe: http://127.0.0.1:8080/api/health      │
   └──────────────────────────┬─────────────────────────────┘
                              │ Internal Docker Network
                              ▼
   ┌────────────────────────────────────────────────────────┐
   │ aurex-postgres (PostgreSQL 16 Alpine)                  │
   │  - Port 5432 internal only                             │
   │  - Storage: postgres_data volume                       │
   └────────────────────────────────────────────────────────┘
```

- **Same-Origin API Routing**:
  The frontend uses `/api` base URL; Nginx proxies all `/api/*` traffic directly to `backend:8080/api/*`.
- **SPA Refresh Handling**:
  Nginx includes `try_files $uri $uri/ /index.html;`, ensuring client-side routes (such as `/investigations/TXN001` or `/users`) do not return 404 upon browser reload.
- **API Error Preservation**:
  `proxy_intercept_errors off;` ensures JSON error bodies (400, 401, 403, 404, 500) from Spring Boot are passed straight to Axios and not converted into Nginx error pages.

---

## 🩺 6. Health Checks & Verification

Issue a GET request to `/api/health` through the public Nginx proxy:

```bash
curl -i http://localhost/api/health
```

Expected HTTP 200 OK Response:
```json
{
  "status": "UP",
  "database": "UP"
}
```

If PostgreSQL is down or connection pool is exhausted, the endpoint responds with HTTP 503 Service Unavailable:
```json
{
  "status": "DOWN",
  "database": "DOWN"
}
```

---

## 🔒 7. Production vs. Demo Deployments

| Dimension | Demo / Staging Deployment | Production Deployment |
|---|---|---|
| **Profiles** | `dev` or `prod` with `ENABLE_SWAGGER=true` | `SPRING_PROFILES_ACTIVE=prod`, `ENABLE_SWAGGER=false` |
| **Admin Seed** | `AUREX_SEED_ADMIN_ENABLED=true` (dev only) | `AUREX_PROD_INITIAL_ADMIN_PASSWORD` on first run, then unset |
| **Data Reset** | `docker compose down -v` allowed | Volume deletion prohibited; use logical database backups |
| **Backups** | None required | Scheduled `pg_dump` cron jobs / offsite S3 snapshots |
| **TLS/HTTPS** | Plain HTTP acceptable for internal dev | Mandatory Let's Encrypt / Cloudflare SSL termination |

---

## 🛠️ 8. Troubleshooting Guide

1. **Backend fails to start with `IllegalStateException: Production environment error: JWT_SECRET...`**
   - In the `prod` profile, `JWT_SECRET` must be set and cannot be the default dev placeholder. Provide a 32+ character random secret in `.env`.
2. **Backend fails to start with `Production initial admin bootstrap failed: ... must be at least 12 characters`**
   - `AUREX_PROD_INITIAL_ADMIN_PASSWORD` is shorter than 12 characters. Update it in `.env` to a stronger password.
3. **Cannot log into initial admin after startup**
   - Check `docker compose logs backend`. If users already existed in the database volume, the bootstrap intentionally skips creation to prevent overwrites.
4. **CORS errors in browser console**
   - If accessing the platform via a custom domain (e.g. `http://aurex.mycompany.com`), add that origin to `CORS_ALLOWED_ORIGINS` in `.env` and run `docker compose up -d`.
5. **Browser refresh on a sub-route displays 404**
   - Ensure the frontend request reaches Nginx and `try_files $uri $uri/ /index.html;` is active.
