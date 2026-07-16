# Deployment Guide - ShortBrew 🚢

This guide outlines how to configure, run, and scale ShortBrew in production.

---

## ⚙️ 1. Environment Variables Reference

ShortBrew uses environment variables for configuration. In local development, these are loaded from the root `.env` file. In production, define these in your container orchestrator (e.g. Kubernetes, AWS ECS, Docker Compose).

### 1.1 Application Configuration

| Variable | Description | Default / Local Dev |
| :--- | :--- | :--- |
| `ENV` | Environment identifier (`development` or `production`) | `development` |
| `DEBUG` | Enable verbose debug output | `true` |
| `SECRET_KEY` | Secret used for JWT generation/verification (HMAC256) | `change-me-in-production-please...` |
| `LOG_JSON` | Enable JSON logging format for ingestion by observability | `true` |
| `LOG_LEVEL` | Application logging level (`DEBUG`, `INFO`, `WARN`, `ERROR`) | `INFO` |
| `SHORT_URL_BASE` | The base URL returned in API responses (what users click) | `http://13.49.57.61.nip.io:8080` |
| `CORS_ORIGINS` | Comma-separated list of allowed CORS origins | `http://localhost:3000,http://localhost:5173,...` |

### 1.2 Infrastructure (PostgreSQL, Redis, RabbitMQ)

Both the Spring Boot API and the TypeScript Worker share the same unified environment keys for infrastructure connectivity:

| Variable | Description | Default / Local Dev |
| :--- | :--- | :--- |
| `DB_HOST` | Hostname of the PostgreSQL database | `localhost` / `postgres` |
| `DB_PORT` | Port of the PostgreSQL database | `5432` |
| `DB_NAME` | PostgreSQL database name | `shortbrew` |
| `DB_USER` | PostgreSQL user | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `password` |
| `REDIS_HOST` | Hostname of Redis container/service | `localhost` / `redis` |
| `REDIS_PORT` | Redis port | `6379` |
| `URL_CACHE_TTL_SECONDS` | Cache expiration duration for resolved URLs in Redis | `3600` (1 hour) |
| `RABBITMQ_HOST` | Hostname of RabbitMQ broker service | `localhost` / `rabbitmq` |
| `RABBITMQ_PORT` | RabbitMQ broker port | `5672` |
| `RABBITMQ_USER` | RabbitMQ username | `guest` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `guest` |

---

## 📦 2. Production Containerization

### 2.1 Backend API (Java)
The backend container image is defined in [backend/Dockerfile](file:///Users/eldhosepeter/Documents/Projects/ShortBrew/backend/Dockerfile). It utilizes a multi-stage Docker build:
1. **Build Stage**: Uses `eclipse-temurin:25-jdk` to cache dependencies and build the fat jar via Gradle (`./gradlew bootJar -x test`).
2. **Runtime Stage**: Copies only the built jar to a lightweight `eclipse-temurin:25-jre` image to keep the production image size minimal and secure.

To build the backend image manually:
```bash
docker build -t shortbrew-backend:latest ./backend
```

### 2.2 Analytics Worker (Node.js)
To package the analytics worker for production, create a standard Node.js Dockerfile inside the `worker` directory. Here is the production [worker/Dockerfile](file:///Users/eldhosepeter/Documents/Projects/ShortBrew/worker/Dockerfile) configuration:

```dockerfile
# Stage 1: Build TypeScript
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json tsconfig.json ./
RUN npm ci
COPY src/ ./src/
RUN npm run build

# Stage 2: Production Execution
FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --only=production
COPY --from=builder /app/dist/ ./dist/
USER node
CMD ["node", "dist/main.js"]
```

To build the worker image manually:
```bash
docker build -t shortbrew-worker:latest ./worker
```

---

## 🐳 3. Full Stack Docker Compose Example

For production environments where full orchestration (like Kubernetes) is not needed, you can use a unified `docker-compose.prod.yaml` file to run the entire system. It leverages YAML extension fields (`x-*`) and anchors (`&`/`*`) to share environment variables commonly between services without redundancy.

```yaml
version: '3.8'

# Common environment settings for backend and worker services
x-common-env: &common-env
  DB_HOST: postgres
  DB_PORT: 5432
  DB_NAME: ${POSTGRES_DB}
  DB_USER: ${POSTGRES_USER}
  DB_PASSWORD: ${POSTGRES_PASSWORD}
  REDIS_HOST: redis
  REDIS_PORT: 6379
  RABBITMQ_HOST: rabbitmq
  RABBITMQ_PORT: 5672
  RABBITMQ_USER: ${RABBITMQ_USER}
  RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}

services:
  postgres:
    image: postgres:16-alpine
    restart: always
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./init-db:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    restart: always
    command: redis-server --appendonly yes
    volumes:
      - redisdata:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    restart: always
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    volumes:
      - rabbitdata:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10

  backend:
    build: ./backend
    restart: always
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      <<: *common-env
      ENV: production
      DEBUG: "false"
      SECRET_KEY: ${SECRET_KEY}
      URL_CACHE_TTL_SECONDS: ${URL_CACHE_TTL_SECONDS}
      SHORTBREW_BASE_URL: ${SHORT_URL_BASE}
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/api/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5

  worker:
    build: ./worker
    restart: always
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    environment:
      <<: *common-env

  prometheus:
    image: prom/prometheus:latest
    restart: always
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    depends_on:
      - backend

  grafana:
    image: grafana/grafana:latest
    restart: always
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:-admin}
    volumes:
      - grafanadata:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    depends_on:
      - prometheus

volumes:
  pgdata:
  redisdata:
  rabbitdata:
  grafanadata:
```

---

## 📈 4. Production Scaling Strategies

Because ShortBrew is architected with separate management and analytical operations, it can scale efficiently:

### 4.1 Scaling the API (Redirection Hot Path)
- The Spring Boot REST API is **completely stateless**. User session states exist in the JWT, and URL configuration metadata is fetched from Redis or PostgreSQL.
- You can run multiple instances of the backend service behind a Load Balancer (Nginx, AWS ALB, HAProxy).
- Configure the load balancer to distribute traffic using standard round-robin or least-connections routing.

### 4.2 Scaling the Workers
- Under high redirection volume, the RabbitMQ `click_events.process` queue will fill up with click tracking messages.
- You can scale the Node.js **Worker** service horizontally (running 2, 5, or 10 instances).
- RabbitMQ will automatically load-balance incoming click events round-robin across all listening workers.
- **Prefetch Limits**: Tune `WORKER_PREFETCH_COUNT` (default `10`). A higher prefetch count increases processing throughput but can consume more database connections per worker.

### 4.3 Database and Redis Scaling
- **Redis**: As caching and rate-limiting are CPU memory-bound, configure Redis in cluster mode or utilize replicas if memory consumption or connection counts saturate a single node.
- **PostgreSQL**: To manage connection limits, use connection poolers like **PgBouncer** in front of your PostgreSQL instance, especially when scaling workers to high numbers.

---

## 🚨 5. Security Checklist

Before taking ShortBrew live, ensure:
1. [ ] **Update Secrets**: Set `SECRET_KEY` to a strong, random, cryptographically secure 256-bit string.
2. [ ] **Disable Debug Logging**: Set `DEBUG=false` to avoid leaking stack traces or SQL details to logs.
3. [ ] **Firewall Ports**: Do not expose PostgreSQL (`5432`), Redis (`6379`), or RabbitMQ (`5672`, `15672`) directly to the public internet. Access should be restricted strictly within your virtual private cloud (VPC).
4. [ ] **Configure HTTPS**: Terminate SSL certificates at your load balancer/gateway before forwarding requests to the API.
5. [ ] **Volume Backups**: Configure scheduled daily snapshot backups for the PostgreSQL `pgdata` volume to protect click history and user data.
