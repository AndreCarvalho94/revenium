# Revenium — README (English)

Summary

This repository contains a prototype for real-time API usage metering and windowed aggregation. The goal was to demonstrate a production-like flow: ingest events, accumulate hot state in Redis, coordinate distributed window closing, publish close events to Kafka, and persist finalized aggregations in Postgres.

What was implemented

- REST ingestion that persists usage events and forwards them to the accumulator.
- `UsageAccumulator` updates Redis hashes per aggregation window (summary, by-endpoint, by-model) and marks windows in a ZSET (`usage:open_windows`).
- `KeyBaseBuilder` centralizes Redis key formats and parsing for `usage:win:<tenant>:<customer>:<startEpoch>` and derived keys (`:summary`, `:byEndpoint:*`, `:byModel:*`).
- Atomic claim of ready windows via a Lua script executed from Java (uses `ZRANGEBYSCORE ... LIMIT` + `ZREM` inside the script). This prevents multiple scheduler instances from claiming the same window.
- `WindowScheduler` polls Redis, uses the atomic claim script to claim windows and publishes `WindowCloseEvent` messages to Kafka (topic `usage.window.close`).
- `CloseWindowProducer` serializes `WindowCloseEvent` and sends it to Kafka.
- `WindowWorker` consumes `usage.window.close`, reads the aggregated data from Redis and persists it to Postgres.
- JPA entity `AggregationWindow`, repository and `AggregationWindowService.persistAggregation(...)` implement a simple upsert by (tenantId, customerId, windowStart). The aggregation JSON is saved in a JSONB column.
- Endpoints to query aggregations:
  - `GET /aggregations/current?tenantId=&customerId=` — read the current aggregation from Redis (if present).
  - `GET /aggregations?tenantId=&customerId=&from=&to=` — list persisted aggregation windows from Postgres (from/to optional).
- Multiple mains so components can be run in isolation: API (`ReveniumApplication`), Scheduler (`ReveniumSchedulerApplication`) and Worker (`ReveniumWorkerApplication`).

Key files and where to look

- Redis keys / helpers: `src/main/java/br/com/acdev/revenium/components/KeyBaseBuilder.java`
- Ingestion and event persistence: `UsageEventService`, `UsageEventController` and `UsageEvent` entity.
- Accumulator (hot path / Redis): `src/main/java/br/com/acdev/revenium/service/UsageAccumulator.java`
- Atomic claim (scheduler): `src/main/java/br/com/acdev/revenium/service/ClaimReadyWindowToClose.java` (Lua via `DefaultRedisScript`).
- Scheduler: `src/main/java/br/com/acdev/revenium/scheduler/WindowScheduler.java`.
- Producer Kafka: `src/main/java/br/com/acdev/revenium/kafka/CloseWindowProducer.java` (topic `usage.window.close`).
- Worker (consumer + persist): `src/main/java/br/com/acdev/revenium/worker/WindowWorker.java`.
- Aggregation persistence: `AggregationWindow` entity, repository and `AggregationWindowService`.
- Event DTO: `WindowCloseEvent` (`src/main/java/br/com/acdev/revenium/kafka/WindowCloseEvent.java`).
- Controller for queries: `AggregationController`.

How the pipeline works (short)

1. Ingest: an event hits the API and is saved in Postgres (durability) and passed to the accumulator.
2. Accumulate (Redis): `UsageAccumulator` updates per-window hashes and ensures the base window key is present in `usage:open_windows` with score = windowEnd (epoch seconds).
3. Claim (scheduler): `WindowScheduler` repeatedly calls the atomic claim script which selects up to N ZSET members with score <= now and removes them atomically, returning the removed members.
4. Publish: each claimed window is published to Kafka as a `WindowCloseEvent`.
5. Persist: `WindowWorker` consumes the `WindowCloseEvent`, reads the aggregated data from Redis via `AggregationWindowService.readCurrentAggregation(...)`, and calls `persistAggregation(...)` to persist the aggregation to Postgres.

Idempotency and race conditions

- Database idempotency: Postgres holds a unique constraint on `(tenant_id, customer_id, window_start)`. That allows the persistence layer to be idempotent (the same aggregation can be saved multiple times without creating duplicates). In production a DB-level upsert (`INSERT ... ON CONFLICT DO UPDATE`) is recommended.
- Distributed coordination: Redis + Lua script provides the coordination layer that prevents multiple scheduler instances from processing the same window. This reduces duplicate Kafka publications and duplicated work on the consumer side.
- Durability caveat: a claim (remove from ZSET) followed by a crash before publishing could lead to a window not being published. The current scheduler attempts to re-queue on immediate publish failures, but for stronger durability a handoff pattern should be used (atomically move the claimed member into a processing queue/stream inside the script).

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

Running locally

Requirements: Java 21, Maven, Redis, Kafka, Postgres.

Build:

```bash
mvn -DskipTests package
```

Mains (IDE or Maven):

- API: `br.com.acdev.revenium.ReveniumApplication`
- Scheduler: `br.com.acdev.revenium.ReveniumSchedulerApplication` (WebApplicationType.NONE)
- Worker: `br.com.acdev.revenium.ReveniumWorkerApplication` (WebApplicationType.NONE)

Useful endpoints

- `POST /usage-events` — ingestion endpoint (see `UsageEventController` for payload)
- `GET /aggregations/current?tenantId=<>&customerId=<>` — read the current aggregation from Redis
- `GET /aggregations?tenantId=<>&customerId=<>[&from=<ISO_INSTANT>&to=<ISO_INSTANT>]` — list persisted aggregation windows

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

ASCII fallback (if Mermaid is not available)

```
API (UsageEventController)
  ├─> Postgres (UsageEvent)            (events persisted)
  └─> UsageAccumulator
         ├─> Redis hashes: <base>:summary, :byEndpoint:calls/tokens, :byModel:calls/tokens
         └─> ZSET: usage:open_windows (score = windowEnd)

Scheduler(s)
  └─> ClaimReadyWindowToClose (Lua: ZRANGEBYSCORE 0..now LIMIT N; ZREM members atomically)
        ├─ removed members -> publish WindowCloseEvent -> Kafka topic usage.window.close
        └─ if publish fails -> requeue to ZSET (retry)

Kafka topic usage.window.close
  └─> WindowWorker (consumer)
        ├─ reads aggregations from Redis (summary + sub-aggregations)
        └─ persists AggregationWindow to Postgres (upsert by tenant/customer/windowStart)

Notes:
- Postgres unique constraint (tenantId, customerId, windowStart) provides idempotency for persisted windows.
- Redis Lua script provides atomic claim to avoid duplicate claims across multiple scheduler instances.
- Durability caveat: claim → crash before publish can cause loss; recommended mitigation: atomic handoff (ZREM + RPUSH/XADD) or outbox/CDC.
```

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

