# Revenium Senior Kotlin/Spring Boot Take-Home Project

## Project: Real-Time API Metering & Aggregation Engine

### Overview
Build a simplified but production-ready API metering and aggregation engine that demonstrates your ability to work with our core technology stack.
This system will process high-volume API usage events, maintain distributed state in Redis, persist data in Postgres, and produce accurate aggregations across multiple service instances.

---

## Business Context
Our platform meters API and AI usage in real-time, aggregating usage data for cost attribution and analytics.
A single customer might generate 10,000+ events per second across multiple API endpoints, and we need accurate aggregations even when running multiple service instances.

- Redis provides real-time state management.
- Postgres stores historical data and configuration.

---

## Technical Requirements

### Core Components

#### 1. Event Ingestion Service
REST endpoint that accepts metering events in this format:

```json
{
  "eventId": "uuid",
  "timestamp": "ISO-8601",
  "tenantId": "string",
  "customerId": "string",
  "apiEndpoint": "/api/completion",
  "metadata": {
    "tokens": 1500,
    "model": "gpt-4",
    "latencyMs": 234,
    "inputTokens": 500,
    "outputTokens": 1000
  }
}
```

Requirements:
- Handle at least 1,000 events/second per instance.
- Process events asynchronously (non-blocking).

#### 2. JPA Data Model (Postgres)
Design and implement a proper domain model with JPA entities:

- Tenant
  - Fields: id, name, active (status), created, updated
- Customer
  - Fields: id, tenantId, externalId, name, created, updated
  - Relationship: Many-to-one with Tenant
- UsageEvent
  - Fields: id, eventId, tenantId, customerId, timestamp, endpoint, metadata (JSONB)
  - Notes: include tokens, model, latency in metadata as needed
- AggregationWindow
  - Fields: id, tenantId, customerId, windowStart, windowEnd, totalCalls, totalTokens, avgLatency, aggregationData (JSONB)
  - Constraint: unique (tenantId, customerId, windowStart)

Other requirements:
- Proper indexing strategy for query patterns.
- Multi-tenancy using column discriminators.
- Proper JPA relationships and fetch strategies.
- Audit fields (created, updated).
- JSONB for flexible metadata.
- Optimistic locking where appropriate.

#### 3. Distributed State Management (Redis)
Use Redis for real-time state:

- Per-customer token/call counters (with TTL).
- Real-time usage tracking across multiple dimensions.
- Window coordination locks.
- Must handle concurrent updates from multiple service instances correctly.

Important: Use Redis for hot data, Postgres for cold/historical data.

#### 4. Batch Aggregation
Accumulate events in 30-second windows and produce aggregated output:

```json
{
  "windowStart": "timestamp",
  "windowEnd": "timestamp",
  "tenantId": "string",
  "customerId": "string",
  "aggregations": {
    "totalCalls": 1543,
    "totalTokens": 234567,
    "totalInputTokens": 78901,
    "totalOutputTokens": 155666,
    "avgLatencyMs": 245,
    "byEndpoint": {
      "/api/completion": { "calls": 1200, "tokens": 180000 },
      "/api/embedding": { "calls": 343, "tokens": 54567 }
    },
    "byModel": {
      "gpt-4": { "calls": 800, "tokens": 150000 },
      "gpt-3.5-turbo": { "calls": 743, "tokens": 84567 }
    }
  }
}
```

- Aggregations must be persisted to Postgres as AggregationWindow entities.

---

## Technical Challenges (Choose Your Approach)

### Challenge 1: Multi-Tenancy Aspect
Implement an AOP-based approach that:

- Validates tenant context automatically on method boundaries.
- Prevents tenant data leakage at the JPA layer.
- Works across service boundaries.
- Has minimal performance overhead.

Considerations:
- How to ensure JPA queries are automatically filtered by tenant?
- Document design decisions (pointcuts, scalability, etc.).

### Challenge 2: Distributed Lock Implementation
Implement a Redis-based distributed lock for coordinating batch window closures across instances.

Requirements:
- Prevent duplicate processing of the same window.
- Handle instance failures gracefully (lock expiration).
- Configurable timeout/retry behavior.
- Include metrics for lock acquisition.
- Provide benchmarks on lock contention vs throughput.

### Challenge 3: Accurate Aggregation Under Concurrency
Ensure accurate aggregation when:

- Multiple instances ingest events for the same customer.
- Windows are closing while new events arrive.
- Late events arrive after window close.

Document your strategy:
- How do you handle late events?
- What consistency guarantees are provided?

### Challenge 4: Hot vs. Cold Path Optimization
Design a strategy for:

- Hot path: Events flow through Redis (real-time aggregation, minimal DB writes).
- Cold path: Completed windows persisted to Postgres.

Event persistence questions:
- When do you write events to Postgres? (Async, batch, or every event?)
- What happens if service crashes mid-window?
- How to balance durability vs throughput?

---

## Data Model Requirements

### Schema Management
- Use Flyway or Liquibase for migrations.
- Provide initial schema migration(s).
- Include seed data for testing.

### JPA Best Practices
- Proper annotations (@Entity, @Table, @Column).
- Correct use of FetchType.LAZY vs EAGER.
- @Transactional with proper propagation.
- Lifecycle callbacks (@PrePersist, etc.).
- Proper equals / hashCode.

### Indexing Strategy
Implement and document indexes for:

- Tenant-based queries.
- Time-range queries on usage events.
- Customer lookups.
- Window uniqueness constraints.

### Query Patterns
Repository methods for:

- Finding customers by tenant.
- Querying usage events by time range and customer.
- Finding aggregation windows for reporting.

Bonus:
- Use JPA Specifications or QueryDSL for complex queries.

---

## Containerization Requirements

### 1. Application Dockerfile
Create a production-ready Dockerfile that:

- Uses multi-stage build (if it makes sense).
- Runs as non-root user.
- Includes health checks.
- Optimizes layer caching.
- Final image < 250 MB (document otherwise).

### 2. Docker Compose Setup
Provide a docker-compose.yml including:

- Application (scalable to multiple instances).
- Postgres (with volume persistence).
- Redis (with persistence).
- Optional monitoring tools.

Must:
- Wait for Postgres readiness.
- Run migrations automatically.
- Seed initial data if needed.

### 3. Container Configuration
- Configurable via environment variables.
- No hardcoded credentials or URLs.
- Support for profiles: dev, test, prod.
- Graceful shutdown (SIGTERM).
- Proper connection pooling.

---

## What We're Evaluating

### Required Deliverables
- Working Spring Boot (Kotlin) app.
- Dockerfile.
- docker-compose.yml.
- README with setup instructions.
- API documentation (Swagger/OpenAPI).
- Data Model Documentation (ER diagram).
- Multi-tenancy design explanation.
- Architecture document (2–3 pages):
  - System design diagram.
  - Data flow.
  - Key design decisions & tradeoffs.
  - Scalability considerations.
  - What you’d improve with more time.

### Testing Strategy
- Unit tests for critical paths.
- Integration tests (Testcontainers: Postgres + Redis).
- Performance test (throughput verification).
- Load testing results (k6, JMeter, etc.).
