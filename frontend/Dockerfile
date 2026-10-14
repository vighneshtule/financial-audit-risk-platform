# Multi-Stage Dockerfile for AUREX React Frontend SPA

# Stage 1: Build static assets
FROM node:20-alpine AS builder
WORKDIR /app

# Cache package dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build production bundle
COPY . .
# When building for containerized deployment, default to same-origin /api
ENV VITE_API_BASE_URL=/api
RUN npm run build

# Stage 2: Minimal unprivileged Nginx runtime
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default Nginx welcome site
RUN rm -rf ./*

# Copy built static bundle from builder stage
COPY --from=builder /app/dist ./

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
