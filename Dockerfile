# Multi-Stage Dockerfile for AUREX Spring Boot Backend

# Stage 1: Build JAR artifact using Maven
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Java Runtime Environment
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root service user for production container security
RUN addgroup -S aurexgroup && adduser -S aurexuser -G aurexgroup

COPY --from=builder /build/target/financial-audit-risk-platform-1.0-SNAPSHOT.jar app.jar
RUN chown -R aurexuser:aurexgroup /app

USER aurexuser

EXPOSE 8080
ENV PORT=8080

# Profile is supplied at runtime via SPRING_PROFILES_ACTIVE environment variable.
# docker-compose.yml injects: SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
# For production deployments set SPRING_PROFILES_ACTIVE=prod in your environment.
# Do NOT hardcode a profile here — it would override the container env var.
ENTRYPOINT ["java", "-jar", "app.jar"]
