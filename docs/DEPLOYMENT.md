# AUREX — Deployment & Operations Guide

This guide provides step-by-step instructions for deploying the **AUREX Financial Audit Risk Intelligence Platform** in staging and production environments.

---

## 📋 Production Requirements

- **Java Runtime**: JDK 17 (Eclipse Temurin recommended)
- **Database**: PostgreSQL 16+
- **Node.js** (Build stage only): Node 20+ & npm
- **Container Environment** (Optional): Docker 24+ & Docker Compose 2+

---

## 🗄️ 1. PostgreSQL Database Setup

1. Provision a PostgreSQL 16 instance.
2. Create the target database:
   ```sql
   CREATE DATABASE financial_audit;
   CREATE USER aurex_user WITH ENCRYPTED PASSWORD 'your_strong_password';
   GRANT ALL PRIVILEGES ON DATABASE financial_audit TO aurex_user;
   ```
3. Database tables and indexes will auto-initialize upon initial Spring Boot startup (`spring.sql.init.mode=always`). In managed DB environments, run [`src/main/resources/schema.sql`](file:///e:/Vighnesh/financial-audit-risk-platform/src/main/resources/schema.sql) directly via `psql`.

---

## ⚙️ 2. Environment Variables Configuration

Configure environment variables in your deployment environment or container service:

| Variable | Recommended Production Value | Description |
|---|---|---|
| `PORT` | `8080` | Service Listener Port |
| `DB_URL` | `jdbc:postgresql://<db-host>:5432/financial_audit` | PostgreSQL Connection String |
| `DB_USERNAME` | `aurex_user` | Production DB User |
| `DB_PASSWORD` | `<secure-password>` | Production DB Password |
| `CORS_ALLOWED_ORIGINS` | `https://aurex.yourdomain.com` | Allowed Frontend Domain(s) |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates `application-prod.properties` |
| `ENABLE_SWAGGER` | `false` | Disable public Swagger UI in Prod |

---

## 📦 3. Backend Deployment (Standalone JAR)

1. Build the production executable JAR:
   ```bash
   mvn clean package -DskipTests
   ```
2. Copy `target/financial-audit-risk-platform-1.0-SNAPSHOT.jar` to target server.
3. Launch service using production profile:
   ```bash
   export DB_URL="jdbc:postgresql://prod-db-host:5432/financial_audit"
   export DB_USERNAME="aurex_user"
   export DB_PASSWORD="securepassword"
   export CORS_ALLOWED_ORIGINS="https://aurex.yourdomain.com"
   export SPRING_PROFILES_ACTIVE="prod"

   java -jar app.jar
   ```

---

## 🐳 4. Docker Container Deployment

### Single Backend Container Build
```bash
docker build -t aurex-backend:latest .
docker run -d \
  -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/financial_audit" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="postgrespassword" \
  -e CORS_ALLOWED_ORIGINS="http://localhost:5173" \
  --name aurex-api aurex-backend:latest
```

### Full-Stack Docker Compose Deployment
```bash
docker-compose up -d --build
```

---

## 🌐 5. Frontend Production Build & Hosting

1. Configure environment file for Vite build:
   Create `frontend/.env.production`:
   ```env
   VITE_API_BASE_URL=https://api-aurex.yourdomain.com/api
   ```
2. Build optimized static assets:
   ```bash
   cd frontend
   npm ci
   npm run build
   ```
3. Deploy the resulting `frontend/dist` directory to Nginx, AWS S3 + CloudFront, Render, or Vercel.

### Sample Nginx Configuration (`/etc/nginx/conf.d/aurex.conf`)
```nginx
server {
    listen 80;
    server_name aurex.yourdomain.com;
    root /var/www/aurex/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 🩺 6. Health & Readiness Verification

Verify system readiness by issuing a GET request to `/api/health`:

```bash
curl -i http://localhost:8080/api/health
```

Expected Output (HTTP 200 OK):
```json
{
  "status": "UP",
  "database": "UP"
}
```
