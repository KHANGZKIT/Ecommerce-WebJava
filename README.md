# Observable Microservices E-Commerce Backend

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.0.0-6DB33F?style=flat-square&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-3.6-231F20?style=flat-square&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

A production-style microservices e-commerce backend built with **Spring Boot 3**, **Java 17**, and **PostgreSQL**. Seven independently deployable services communicate through a central **Spring Cloud Gateway**. The checkout flow is orchestrated as a saga: synchronous payment processing followed by asynchronous stock decrements and Kafka-driven notifications.

> **Project status:** Core service development is in progress. Full observability (distributed tracing, metrics, structured logging) is a planned phase and will be layered in once all services are complete — hence the project name.

---

## Table of Contents

- [System Architecture](#system-architecture)
- [Services Overview](#services-overview)
- [Key Design Decisions](#key-design-decisions)
- [Kafka Topics](#kafka-topics)
- [API Reference](#api-reference)
- [Security Architecture](#security-architecture)
- [Database Schema](#database-schema)
- [Tech Stack](#tech-stack)
- [Running Locally](#running-locally)
- [Environment Variables](#environment-variables)
- [Production Considerations](#production-considerations)
- [Roadmap](#roadmap)

---

## System Architecture

```
                          +------------------------------------------+
         Client Requests  |              API Gateway  :8080           |
         ───────────────► |  Spring Cloud Gateway · JWT Filter        |
                          |  Redis-backed rate limiting · CORS        |
                          +--------------------+---------------------+
                                               │ Routes by path prefix
            +──────────────────────────────────┼──────────────────────────────+
            │                                  │                              │
            ▼                                  ▼                              ▼
  +──────────────────+           +──────────────────+          +──────────────────────+
  │   User Service   │           │ Product Service  │          │    Cart Service      │
  │   :8081          │           │  :8082           │          │    :8083             │
  │  Register/Login  │           │  Catalog         │          │  Add / Remove items  │
  │  JWT issuance    │  Redis ◄──┤  Categories      │──► Redis │  Qty update          │
  │  Profile CRUD    │           │  Inventory       │          │  Auto-total          │
  +──────────────────+           +──────────────────+          +──────────────────────+
         │ own DB                        ▲  WebClient                  │ WebClient
         ▼                              │                              ▼
    [user_db]            +─────────────────────────────────────────────────────+
                         │              Order Service  :8084                   │
                         │  Checkout saga orchestrator                         │
                         │  1. Batch fetch products                            │
                         │  2. Persist PENDING order                           │
                         │  3. Synchronous payment call                        │
                         │  4. Async stock decrements (WebClient)              │
                         │  5. Publish Kafka event (fire-and-forget)           │
                         +───────────────┬─────────────────────────────────────+
                                         │
                    ┌────────────────────┴─────────────────────┐
                    ▼                                           ▼
     +──────────────────────+                   +─────────────────────────────+
     │   Payment Service    │                   │   Notification Service      │
     │   :8085              │                   │   :8086                     │
     │   Mock charge/refund │                   │   Kafka consumer            │
     │   90 % success rate  │                   │   Mock email/SMS dispatch   │
     +──────────────────────+                   │   Audit log of sends        │
                                                +─────────────────────────────+

                               ┌────────────────────────┐
                               │   Shared Middleware     │
                               │  Redis 7  (cache/rate)  │
                               │  Kafka + Zookeeper 7.6  │
                               └────────────────────────┘
```

---

## Services Overview

| Service              | Port | Database         | Responsibility                                       |
|----------------------|------|------------------|------------------------------------------------------|
| API Gateway          | 8080 | —                | Route all requests, JWT validation, CORS, rate limit |
| User Service         | 8081 | `user_db`        | Register, Login (JWT), Profile CRUD                  |
| Product Service      | 8082 | `product_db`     | Catalog, Categories, Inventory management            |
| Cart Service         | 8083 | `cart_db`        | Per-user cart, denormalised product snapshots        |
| Order Service        | 8084 | `order_db`       | Checkout saga (product → payment → Kafka notify)     |
| Payment Service      | 8085 | `payment_db`     | Mock payment processing and refunds                  |
| Notification Service | 8086 | `notification_db`| Kafka-driven email/SMS dispatch and audit log        |

---

## Key Design Decisions

### 1. JWT Validation at the Gateway

JWT tokens (HS256, 24-hour expiry) are issued by the User Service and validated **once** at the API Gateway. The gateway injects `X-User-Email` and `X-Auth-Token` headers into all downstream requests. Downstream services never handle raw JWTs — they rely solely on these trusted headers.

### 2. Database-per-Service

Each service owns its schema exclusively. There are no cross-service JOINs or shared tables. This enforces bounded contexts and allows each service to evolve its schema and scale independently.

### 3. Denormalised Snapshots in Cart and Order Items

Cart items and order items capture a snapshot of product data (name, SKU, price) at the time of the add or checkout event. This preserves historical accuracy — a subsequent price change on a product does **not** alter past carts or completed orders.

### 4. Checkout Saga (Hybrid Orchestration)

The Order Service orchestrates the checkout saga in the following sequence:

1. **Batch-fetch** all product details in a single WebClient call (eliminates N+1 remote calls)
2. Validate stock availability for every item
3. Persist the order with status `PENDING`
4. Call the Payment Service **synchronously** — the result determines order status
5. On `SUCCESS`: set status to `CONFIRMED`, **asynchronously** decrement each product's stock (WebClient, fire-and-forget)
6. Publish a Kafka event (`order.confirmed` or `order.payment-failed`) — the Notification Service consumes it asynchronously

> **Why hybrid?** Payment is synchronous because the API response must reflect success or failure. Stock decrements and notifications are asynchronous because they are non-fatal background side-effects that should not block the client.

### 5. Kafka-Driven Notifications

The Notification Service is a pure Kafka consumer. It listens on `order.confirmed` and `order.payment-failed` topics, constructs and dispatches the appropriate notification, and persists an audit record — all without being coupled to the Order Service via HTTP.

### 6. Redis for Caching and Rate Limiting

Redis is shared across the API Gateway (rate limiting) and the product/user/cart services (response caching). Each service connects to the same Redis instance via Lettuce with a bounded connection pool.

### 7. Local DTOs — No Shared Library

Each service declares its own local DTOs for inter-service calls. This avoids shared-library coupling, keeps each service independently deployable, and allows DTOs to evolve at different rates.

---

## Kafka Topics

| Topic                 | Producer      | Consumer             | Payload                                          |
|-----------------------|---------------|----------------------|--------------------------------------------------|
| `order.confirmed`     | Order Service | Notification Service | `orderId`, `userId`, `transactionId`, `totalAmount` |
| `order.payment-failed`| Order Service | Notification Service | `orderId`, `userId`, `reason`                    |

Consumer group: `notification-service-group` (ensures exactly-once processing per message when scaled horizontally).

---

## API Reference

### OpenAPI / Swagger UI

The full API is documented via **springdoc-openapi**. After starting all containers, access the centralised Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

Use the **"Select a definition"** dropdown (top-right) to switch between the APIs of each microservice without changing ports.

---

### Authentication — Public (no JWT required)

```
POST  /api/v1/auth/register     Register a new user account and receive a JWT
POST  /api/v1/auth/login        Authenticate with credentials and receive a JWT
```

### Users — JWT Required

```
GET    /api/v1/users/me         Retrieve the authenticated user's profile
PATCH  /api/v1/users/me         Update the authenticated user's profile
GET    /api/v1/users            [ADMIN] List all users
GET    /api/v1/users/{id}       [ADMIN] Get user by ID
DELETE /api/v1/users/{id}       [ADMIN] Delete user by ID
```

### Products

```
GET    /api/v1/products                    Paginated list of active products
GET    /api/v1/products/{id}              Get product by ID
GET    /api/v1/products/sku/{sku}         Get product by SKU
GET    /api/v1/products/category/{catId}  Filter products by category
GET    /api/v1/products/search?keyword=   Full-text keyword search
POST   /api/v1/products                   Create a product
POST   /api/v1/products/batch             [Internal] Batch-fetch products by ID list (used by order-service)
PATCH  /api/v1/products/{id}             Partial update of a product
PATCH  /api/v1/products/{id}/inventory   Update stock (ADD / SUBTRACT / SET)
DELETE /api/v1/products/{id}             Soft-delete a product
```

### Categories

```
GET    /api/v1/categories         List all active categories
GET    /api/v1/categories/{id}    Get category by ID
POST   /api/v1/categories         Create a category
PUT    /api/v1/categories/{id}    Update a category
DELETE /api/v1/categories/{id}    Soft-delete a category
```

### Cart — JWT Required (resolved via Gateway header)

```
GET    /api/v1/cart                  Retrieve the authenticated user's cart
POST   /api/v1/cart/items            Add an item (merges quantity if duplicate)
PATCH  /api/v1/cart/items/{itemId}   Update item quantity (quantity = 0 removes it)
DELETE /api/v1/cart/items/{itemId}   Remove a specific item
DELETE /api/v1/cart                  Clear the entire cart
```

### Orders — JWT Required (resolved via Gateway header)

```
POST   /api/v1/orders                  Initiate checkout (triggers full saga)
GET    /api/v1/orders/{id}             Get order by ID
GET    /api/v1/orders/user/{userId}    Paginated order history for a user
POST   /api/v1/orders/{id}/cancel      Cancel an order (PENDING or CONFIRMED only)
PATCH  /api/v1/orders/{id}/status      [ADMIN] Manually update order status
```

### Payments

```
POST   /api/v1/payments/charge         Process a payment charge
POST   /api/v1/payments/refund         Refund a successful payment
GET    /api/v1/payments/{id}           Get payment by ID
GET    /api/v1/payments/order/{id}     Get all payments for an order
GET    /api/v1/payments/user/{id}      Get payment history for a user
```

### Notifications

```
POST   /api/v1/notifications/send                 Send a notification manually
GET    /api/v1/notifications/reference/{id}       Get notifications by reference ID
GET    /api/v1/notifications/recipient?email=     Get notifications by recipient email
```

---

## Security Architecture

```
JWT issued by user-service (HS256)
  Expiry: configurable via JWT_EXPIRATION (default 30 min; .env template sets 24 h)
    │
    └─► Validated by API Gateway on every inbound request
             │
             └─► Two trusted headers injected into downstream requests:
                   X-User-Email  — used by user-service and order-service
                   X-User-Id     — used by cart-service
                      │
                      └─► Downstream services trust these headers (no re-validation)
```

| Mechanism          | Implementation                                            |
|--------------------|-----------------------------------------------------------|
| Password hashing   | BCrypt (strength 10)                                      |
| JWT algorithm      | HS256, signed with `JWT_SECRET` environment variable      |
| JWT expiry         | Configurable via `JWT_EXPIRATION` (ms); default 30 min    |
| Session management | Stateless (no `HttpSession`)                              |
| Roles              | `CUSTOMER` (default) and `ADMIN`                          |
| Role authorisation | `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints    |
| Input validation   | Jakarta Bean Validation on all request DTOs               |
| Type coercion      | Rejected — JSON field types are strictly enforced         |

---

## Database Schema

| Service              | Key Tables                                                                            |
|----------------------|---------------------------------------------------------------------------------------|
| user-service         | `users` (id, email, password_hash, role, enabled)                                     |
| product-service      | `products` (id, sku, price, stock_quantity, category_id, active), `categories`        |
| cart-service         | `carts` (id, user_id, total_price), `cart_items` (product snapshot per line)          |
| order-service        | `orders` (id, user_id, status, total_amount, payment_txn_id), `order_items`           |
| payment-service      | `payments` (id, transaction_id, order_id, amount, status, method)                     |
| notification-service | `notifications` (id, recipient, subject, type, status, reference_id, sent_at)         |

Schema management: **Hibernate DDL auto = `update`** — tables are created or migrated on startup.

---

## Tech Stack

| Concern             | Technology                                  |
|---------------------|---------------------------------------------|
| Language            | Java 17                                     |
| Framework           | Spring Boot 3.2.3                           |
| Gateway             | Spring Cloud Gateway 2023.0.0               |
| Security            | Spring Security 6 + JWT (jjwt 0.11.5)      |
| Persistence         | Spring Data JPA + Hibernate                 |
| Database            | PostgreSQL 16 (one instance per service)    |
| Caching             | Redis 7 (Lettuce client, connection pooled) |
| Messaging           | Apache Kafka 3.6 + Zookeeper 7.6            |
| HTTP Client         | Spring WebFlux `WebClient`                  |
| Validation          | Jakarta Bean Validation                     |
| Mapping             | MapStruct 1.5.5                             |
| API Docs            | springdoc-openapi 2.3.0 (Swagger UI)        |
| Boilerplate         | Lombok 1.18.30                              |
| Containerisation    | Docker + Docker Compose                     |
| Build               | Maven (multi-module parent POM)             |

---

## Running Locally

### Prerequisites

- **Docker Desktop** with Compose V2 (`docker compose` — not `docker-compose`)
- **Java 17** and **Maven 3.9+** to build JARs before Docker image creation
- **~4 GB of free RAM** — each of the 7 application containers is capped at 512 MB; add the 6 PostgreSQL instances, Redis, and Kafka/Zookeeper

### Step 1: Configure the environment file

Create a `.env` file in the project root (it is listed in `.gitignore` and **must not be committed**):

```bash
POSTGRES_USER=postgres
POSTGRES_PASSWORD=YourSecurePassword!
JWT_SECRET=<64-char-hex-string>
JWT_EXPIRATION=86400000
COMPOSE_PARALLEL_LIMIT=3
```

> **Tip:** Generate a strong JWT secret with `openssl rand -hex 32`.

A reference template is shown above. The `COMPOSE_PARALLEL_LIMIT=3` cap prevents a BuildKit pipe crash that occurs when all 7 services try to build simultaneously against the same large build context.

### Step 2: Build all service JARs (optional)

Each Dockerfile performs a self-contained multi-stage Maven build inside Docker, so a local Maven build is **not required** before `docker compose up --build`. However, running it locally first caches dependencies and speeds up the Docker build significantly:

```bash
# Optional — run from project root; builds all modules via the parent POM
mvn clean package -DskipTests
```

### Step 3: Start all containers

```bash
docker compose up --build
```

Docker Compose will start (in dependency order): 6 × PostgreSQL → Redis → Zookeeper → Kafka → 7 × microservices.

### Step 4: Verify service health

```bash
# Check all container states
docker compose ps

# Verify the API Gateway is healthy
curl http://localhost:8080/actuator/health
```

### Step 5: Register and authenticate

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com",
    "password": "Password1!",
    "phone": "+919876543210"
  }'

# Login to obtain a JWT
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jane@example.com", "password": "Password1!"}'

# Access a protected endpoint
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

### Step 6: End-to-end checkout flow

```bash
TOKEN="<YOUR_JWT_TOKEN>"

# 1. Create a category
curl -X POST http://localhost:8080/api/v1/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Electronics", "description": "Electronics and Gadgets"}'

# 2. Create a product (use the categoryId returned above)
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "sku": "LAP-001", "price": 59999.0, "stockQuantity": 50, "categoryId": 1}'

# 3. Place an order — triggers the full checkout saga
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "shippingAddress": "123 Main Street, Hyderabad, Telangana",
    "paymentMethod": "CREDIT_CARD",
    "items": [{"productId": 1, "quantity": 2}]
  }'
```

### Tear Down

```bash
# Stop and remove all containers; -v also removes named volumes (clears all database data)
docker compose down -v
```

---

## Environment Variables

| Variable                | Description                                          | Example                    |
|-------------------------|------------------------------------------------------|----------------------------|
| `POSTGRES_USER`         | PostgreSQL username used by all database containers  | `postgres`                 |
| `POSTGRES_PASSWORD`     | PostgreSQL password used by all database containers  | `SecurePass!`              |
| `JWT_SECRET`            | HS256 signing secret (min 64 hex characters)         | `404E635266556A58...`      |
| `JWT_EXPIRATION`        | Token expiry in milliseconds                         | `86400000` (24 hours)      |
| `COMPOSE_PARALLEL_LIMIT`| Max concurrent Docker Compose service builds         | `3`                        |

---

## Production Considerations

The following enhancements are recommended before promoting this system to a production environment:

| Area                  | Recommendation                                                           |
|-----------------------|--------------------------------------------------------------------------|
| Kafka Resilience      | Add dead-letter topics for failed Notification consumer messages         |
| Kafka Reliability     | Increase replication factor to ≥ 2 in production (currently 1 — dev default) |
| Resilience            | Add retry and circuit-breaker policies via Resilience4j on WebClient calls |
| Service Discovery     | Integrate Spring Cloud Eureka or switch to Kubernetes DNS                |
| Configuration         | Centralise configuration with Spring Cloud Config Server                 |
| Distributed Tracing   | Add end-to-end tracing with Micrometer Tracing + Zipkin or Tempo         |
| Metrics & Alerting    | Expose Actuator metrics to Prometheus; alert via Grafana                 |
| Payments              | Replace the mock payment processor with Stripe or Razorpay               |
| Notifications         | Integrate JavaMailSender for real email; Twilio for SMS                  |
| Database Migrations   | Replace `ddl-auto: update` with Flyway or Liquibase for controlled migrations |
| Secrets Management    | Store `JWT_SECRET` and DB credentials in Vault or a cloud secrets manager |
| API Versioning        | Enforce versioning via URL path (`/api/v2/`) or `Accept` request headers |

---

## Roadmap

The following capabilities are planned for upcoming development phases:

### Phase 1 — Observability (next milestone)

| Capability              | Planned Implementation                                            |
|-------------------------|-------------------------------------------------------------------|
| Distributed Tracing     | Micrometer Tracing + Zipkin (or Grafana Tempo)                   |
| Metrics Collection      | Micrometer → Prometheus scrape endpoint on every service         |
| Dashboards & Alerting   | Grafana dashboards with RED metrics (Rate, Errors, Duration)      |
| Structured Logging      | Logback JSON encoder; correlation IDs propagated via MDC          |
| Health Aggregation      | Spring Boot Admin or custom Gateway health dashboard              |

### Phase 2 — Hardening

| Capability              | Planned Implementation                                            |
|-------------------------|-------------------------------------------------------------------|
| Resilience              | Resilience4j circuit-breakers and retries on all WebClient calls  |
| Kafka Reliability       | Increase replication factor and add dead-letter topics (DLT)      |
| Database Migrations     | Flyway replacing `ddl-auto: update`                               |
| Secrets Management      | Environment-agnostic secrets via HashiCorp Vault or cloud KMS     |
| Real Payments           | Stripe or Razorpay integration replacing the mock processor       |
| Real Notifications      | JavaMailSender (email) + Twilio (SMS) replacing mock dispatch     |
