# Revenium — README

Summary

This repository contains a prototype for real-time API usage metering and windowed aggregation. The goal was to demonstrate a production-like flow: ingest events, accumulate hot state in Redis, coordinate distributed window closing, publish close events to Kafka, and persist finalized aggregations in Postgres.

What was implemented

- REST ingestion that persists usage events and forwards them to the accumulator.
- `UsageAccumulator` updates Redis hashes per aggregation window (summary, by-endpoint, by-model) and marks windows in a ZSET (`usage:open_windows`).
- `KeyBaseBuilder` centralizes Redis key formats and parsing for `usage:win:<tenant>:<customer>:<startEpoch>` and derived keys (`:summary`, `:byEndpoint:*`, `:byModel:*`).
- Atomic claim of ready windows via a Lua script executed from Java (uses `ZRANGEBYSCORE ... LIMIT` + `ZREM` inside the script). This prevents multiple scheduler instances from claiming the same window.
- `WindowScheduler` (active only on profile `scheduler`) polls Redis, uses the atomic claim script to claim windows and publishes `WindowCloseEvent` messages to Kafka (topic `usage.window.close`).
- `WindowCloseProducer` serializes `WindowCloseEvent` and sends it to Kafka.
- `WindowCloseWorker` (active only on profile `worker`) consumes `usage.window.close`, reads the aggregated data from Redis and persists it to Postgres.
- JPA entity `AggregationWindow`, repository and `AggregationWindowService.persistAggregation(...)` implement a simple upsert by (tenantId, customerId, windowStart). The aggregation JSON is saved in a JSONB column.
- Endpoints to query aggregations:
  - `GET /aggregations/current?tenantId=&customerId=` — read the current aggregation from Redis (if present).
  - `GET /aggregations?tenantId=&customerId=&from=&to=` — list persisted aggregation windows from Postgres (from/to optional).
- Profile-driven startup: a single main (`ReveniumApplication`) runs different roles (API, Scheduler, Worker) based on Spring profiles. Only profile `api` starts a web server.

Key files and where to look

- Redis keys / helpers: `src/main/java/br/com/acdev/revenium/components/KeyBaseBuilder.java`
- Ingestion and event persistence: `UsageEventService`, `UsageEventController` and `UsageEvent` entity.
- Accumulator (hot path / Redis): `src/main/java/br/com/acdev/revenium/service/UsageAccumulator.java`
- Atomic claim (scheduler): `src/main/java/br/com/acdev/revenium/service/ClaimReadyWindowToClose.java` (Lua via `DefaultRedisScript`).
- Scheduler: `src/main/java/br/com/acdev/revenium/scheduler/WindowScheduler.java`.
- Producer Kafka: `src/main/java/br/com/acdev/revenium/kafka/WindowCloseProducer.java` (topic `usage.window.close`).
- Worker (consumer + persist): `src/main/java/br/com/acdev/revenium/worker/WindowCloseWorker.java`.
- Aggregation persistence: `AggregationWindow` entity, repository and `AggregationWindowService`.
- Event DTO: `WindowCloseEvent` (`src/main/java/br/com/acdev/revenium/kafka/WindowCloseEvent.java`).
- Controller for queries: `AggregationController`.
- Scheduler enablement by profile: `config/SchedulerConfig` (enables `@Scheduled` only on profile `scheduler`).

How the pipeline works (short)

1. Ingest: an event hits the API and is saved in Postgres (durability) and passed to the accumulator.
2. Accumulate (Redis): `UsageAccumulator` updates per-window hashes and ensures the base window key is present in `usage:open_windows` with score = windowEnd (epoch seconds).
3. Claim (scheduler): `WindowScheduler` repeatedly calls the atomic claim script which selects up to N ZSET members with score <= now and removes them atomically, returning the removed members.
4. Publish: each claimed window is published to Kafka as a `WindowCloseEvent`.
5. Persist: `WindowWorker` consumes the `WindowCloseEvent`, reads the aggregated data from Redis via `AggregationWindowService.readAggregation(...)`, and calls `persistAggregation(...)` to persist the aggregation to Postgres.

Idempotency and race conditions

- Database idempotency: Postgres holds a unique constraint on `(tenant_id, customer_id, window_start)`. That allows the persistence layer to be idempotent (the same aggregation can be saved multiple times without creating duplicates). In production a DB-level upsert (`INSERT ... ON CONFLICT DO UPDATE`) is recommended.
- Distributed coordination: Redis + Lua script provides the coordination layer that prevents multiple scheduler instances from processing the same window. This reduces duplicate Kafka publications and duplicated work on the consumer side.
- Durability caveat: a claim (remove from ZSET) followed by a crash before publishing could lead to a window not being published. The current scheduler attempts to re-queue on immediate publish failures, but for stronger durability a handoff pattern should be used (atomically move the claimed member into a processing queue/stream inside the script).

Why Spring profiles for roles (api / scheduler / worker)

- Single codebase, single fat JAR: the same artifact runs three roles by selecting profiles. That reduces duplication and keeps classpath, configs, and dependencies in sync.
- Startup shape by profile:
  - Profile `api`: web server (Tomcat) is enabled. Controllers are active, and you can access HTTP endpoints.
  - Profiles `scheduler` and `worker`: run as non-web apps (no embedded Tomcat). The scheduler enables `@Scheduled` tasks only when `scheduler` is active; the worker enables Kafka listeners only when `worker` is active.
- Clear separation of beans:
  - `@Profile("scheduler")` guards scheduler-only components like `WindowScheduler` and scheduling configuration.
  - `@Profile("worker")` guards background consumer components like `WindowCloseWorker`.
- Operational flexibility: you can run any combination (just the API, only workers, multiple schedulers, etc.) by choosing profiles or containers.

Running with Docker Compose (recommended)

Prerequisites: Docker + Docker Compose.

- Build and start the full stack (Postgres, Redis, Kafka, API, Scheduler, Worker):

```bash
docker compose up -d --build
```

- Check the logs:

```bash
docker compose logs -f revenium-app revenium-scheduler revenium-worker
```

- Stop everything:

```bash
docker compose down
```

- Rebuild after code changes:

```bash
docker compose build
# or force rebuild on up
docker compose up -d --build
```

Services and profiles in Compose

- `revenium-app`: runs with `SPRING_PROFILES_ACTIVE=api,local` (web server on 8080)
- `revenium-scheduler`: runs with `SPRING_PROFILES_ACTIVE=scheduler,local` (no web server)
- `revenium-worker`: runs with `SPRING_PROFILES_ACTIVE=worker,local` (no web server)

Environment (overridable in compose):

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`

Local run without Docker

- Build:

```bash
mvn -DskipTests package
```

- Run API (web):

```bash
java -jar target/app.jar --spring.profiles.active=api,local
```

- Run Scheduler (headless):

```bash
java -jar target/app.jar --spring.profiles.active=scheduler,local
```

- Run Worker (headless):

```bash
java -jar target/app.jar --spring.profiles.active=worker,local
```

Useful endpoints (when API is running)

- `POST /usage-events` — ingestion endpoint (see `UsageEventController` for payload)
- `GET /aggregations/current?tenantId=<>&customerId=<>` — read the current aggregation from Redis
- `GET /aggregations?tenantId=<>&customerId=<>[&from=<ISO_INSTANT>&to=<ISO_INSTANT>]` — list persisted aggregation windows

Swagger / OpenAPI (API profile)

- Swagger UI: http://localhost:8080/docs
- OpenAPI JSON: http://localhost:8080/api-docs

With the `revenium-app` service running (profile `api`), open the Swagger UI and use “Try it out” to call each endpoint.

Quick examples (curl)

- Create a usage event:

```bash
curl -X POST http://localhost:8080/usage-events \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "33333333-3333-3333-3333-333333333340",
    "timestamp": "2025-11-03T02:37:50Z",
    "tenantId": "11111111-1111-1111-1111-111111111111",
    "customerId": "22222222-2222-2222-2222-222222222222",
    "apiEndpoint": "/api/completion",
    "metadata": {
      "tokens": 1500,
      "inputTokens": 500,
      "outputTokens": 1000,
      "model": "gpt-4",
      "latencyMs": 234
    }
  }'
```

- Read current aggregation (hot from Redis):

```bash
curl "http://localhost:8080/aggregations/current?tenantId=11111111-1111-1111-1111-111111111111&customerId=22222222-2222-2222-2222-222222222222"
```

- List persisted aggregation windows (Postgres), optional time range:

```bash
# without time range
curl "http://localhost:8080/aggregations?tenantId=11111111-1111-1111-1111-111111111111&customerId=22222222-2222-2222-2222-222222222222"

# with time range (ISO-8601 instants)
curl "http://localhost:8080/aggregations?tenantId=11111111-1111-1111-1111-111111111111&customerId=22222222-2222-2222-2222-222222222222&from=2025-11-03T00:00:00Z&to=2025-11-03T23:59:59Z"
```

Mermaid architecture diagram

Paste the block below into a Mermaid-enabled viewer (GitHub, VSCode Mermaid preview, Mermaid Live Editor) to render the architecture diagram.

```mermaid
flowchart TD
  subgraph API
    A[UsageEventController\n(POST /usage-events)] -->|save event| P[Postgres (UsageEvent)]
    A -->|call| Acc[UsageAccumulator]
  end

  subgraph Redis["Redis (hot state)"]
    Acc -->|HSET / HINCR| H_summary["Hash: <base>:summary"]
    Acc -->|HINCR| H_ep_calls["Hash: <base>:byEndpoint:calls"]
    Acc -->|HINCR| H_ep_tokens["Hash: <base>:byEndpoint:tokens"]
    Acc -->|HINCR| H_model_calls["Hash: <base>:byModel:calls"]
    Acc -->|HINCR| H_model_tokens["Hash: <base>:byModel:tokens"]
    Acc -->|ZADD score=windowEnd| Z[ZSET: usage:open_windows]
  end

  subgraph Scheduler
    S[WindowScheduler\n(periodic)] -->|call| Lua[ClaimReadyWindowToClose\n(Lua: ZRANGEBYSCORE 0..now -> ZREM ...)]
    Lua -->|removed members| S
    S -->|publish WindowCloseEvent| K[Kafka topic: usage.window.close]
    S -->|on publish fail| R[Requeue into ZSET]
  end

  subgraph Kafka
    K -->|consume| W[WindowWorker (Kafka consumer)]
  end

  subgraph Worker
    W -->|read aggregated keys| Redis
    W -->|call| AggService[AggregationWindowService]
    AggService -->|upsert| PG[Postgres (AggregationWindow)]
  end

  note_right_of PG
    DB unique constraint:\n(tenantId, customerId, windowStart)\nprovides idempotency at persist
  end

  note left_of Lua
    Lua atomic selection+remove prevents\nduplicate claims across schedulers
  end

  note bottom_of Scheduler
    Durability caveat:\nclaim -> crash before publish -> potential loss;\nmitigation: atomic handoff to queue (RPUSH/XADD) or outbox
  end

  style Redis fill:#f7f7f7,stroke:#333
  style Scheduler fill:#eef6ff,stroke:#2b7cff
  style Worker fill:#eefbe6,stroke:#2b9f2b
  style API fill:#fff7e6,stroke:#f59e00
  style Kafka fill:#fff0f6,stroke:#d6336c
  style PG fill:#f0f8ff,stroke:#2b7cff
```

What was NOT implemented (time constraints)

- Reprocessing of late events (late-arriving events that fall into already-closed windows) — design notes and a plan are provided below.
- JPA multi-tenant enforcement (automatic tenant scoping at query-level) — not implemented.
- Performance and load tests — not done due to time.

Late-events (reprocessing) design idea

1. Save all events at ingestion in Postgres (already implemented).
2. Add a recovery scheduler that periodically queries Postgres for events that fall outside persisted windows (or that belong to windows already closed).
3. For each affected window, recalculate aggregation(s) reading the events from Postgres for that window range and upsert the AggregationWindow.
4. Use claim mechanics (Redis zset/stream or Lua) to coordinate recovery workers across instances.

Why we keep both Postgres and Redis

- Postgres provides durable storage and idempotency guarantees.
- Redis is the fast coordination/hot-state layer: it keeps the frequently-updated aggregation state and prevents duplicate claims between schedulers.

Next steps & recommended improvements

1. Durability improvements:
   - Implement DB-level upsert (`INSERT ... ON CONFLICT DO UPDATE`) at persistence layer.
   - Implement atomic handoff in the claim script (ZREM + RPUSH/XADD) to guarantee processing persistence before publishing.
2. Late-event recovery:
   - Implement the recovery scheduler described above to reprocess late events and recalculate affected windows.
3. Multi-tenant JPA:
   - Add tenant scoping (Hibernate filters or an AOP-based approach) to automatically add tenant predicates to queries.
4. Observability & resilience:
   - Add metrics and tracing (claimed windows, published windows, persisted windows, failures), health endpoints and alerts.
5. Testing:
   - Integration tests with Testcontainers for Redis, Kafka and Postgres; performance/load tests for accumulator and scheduler.
