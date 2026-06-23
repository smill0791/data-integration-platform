# Demo Guide — Multi-Source Data Integration Platform

This guide walks through how to demo this project at two levels:

- **[Beginner](#beginner-walkthrough)** — What the system does, why it matters, live UI tour. No code required.
- **[Advanced](#advanced-technical-walkthrough)** — Architecture decisions, code patterns, testing strategy, GraphQL deep dive.

---

## Before You Start

Make sure everything is running:

```bash
docker-compose up -d    # SQL Server + LocalStack (SQS)
./start-dev.sh          # All 5 services
```

Confirm services are up:

| URL | What to check |
|---|---|
| http://localhost:3000 | Dashboard loads, shows metric cards |
| http://localhost:8080/health | Returns `{ "status": "UP" }` |
| http://localhost:8080/graphiql | GraphQL playground loads |
| http://localhost:3001/api/customers | Returns paginated JSON |
| http://localhost:3002/api/products | Returns paginated JSON |
| http://localhost:3003/api/invoices | Returns paginated JSON |

---

## Beginner Walkthrough

**Target audience**: Recruiters, product managers, non-technical stakeholders, junior developers.

**Framing to open with**:

> "Imagine a company that runs on three separate software systems — a CRM for customer data, an ERP for inventory, and an accounting platform for invoices. The problem is these systems don't talk to each other. This platform is the bridge. It pulls data from all three, cleans it, validates it, and loads it into a unified database — automatically, reliably, and with a full audit trail."

### Step 1 — Show the Dashboard

Open http://localhost:3000.

Point out:
- The **metric cards** across the top — total syncs in the last 24 hours and 30 days, success rate, total records processed
- The **bar chart** — syncs per day, broken down by completed vs. failed
- The **job table** — each row is one sync run, with its source system, status badge, record counts, and timestamp
- The **source filter pills** — lets you scope the view to CRM, ERP, or Accounting

> "This is the operations dashboard. At a glance, you can see system health. Is data coming in? Are jobs succeeding? Which source is having problems?"

### Step 2 — Trigger a Live Sync

Find the **Source Sync Panel** on the dashboard (the three sync buttons).

1. Click **Sync CRM**
2. The button shows a loading state — the request has been accepted
3. A new row appears in the job table almost immediately, status: **QUEUED** (gray)
4. Within a second or two, it flips to **RUNNING** (blue, pulsing)
5. After a few seconds: **COMPLETED** (green) — or **FAILED** (red) if the mock API's 5% failure rate kicked in

> "The status updates happen in real time without refreshing the page. The system is processing 100 customer records in the background while we watch."

If it fails, that's actually a good demo moment:
> "The mock API randomly fails 5% of the time to simulate real-world unreliability. Notice the job still completes — it processed the records that succeeded and logged the failures. The platform is designed to isolate errors at the individual record level, not crash the whole job."

### Step 3 — Inspect a Job

Click any row in the job table to open the job detail page.

Point out:
- **Job summary** — source, status, start/end time, duration, records processed vs. failed
- **Error log table** — if any records failed, each error is listed with the error type, message, and the raw data that caused it

> "Every sync is fully audited. If something goes wrong at 2am, you can come back the next morning, open the job, and see exactly which records failed, why they failed, and what the raw data looked like."

### Step 4 — Show the Three Sources

Trigger a sync from each source panel (CRM, ERP, Accounting) and let them run while you explain:

> "CRM syncs customer records — names, emails, phone numbers, addresses. ERP syncs product catalog data — SKUs, prices, inventory counts. Accounting syncs invoices — amounts, statuses, due dates, line items. All three go through the same pipeline: pulled from the source API, staged, transformed, validated, and loaded into the database."

### Step 5 — Closing Frame

> "The whole system runs on Docker locally, but it's designed for production. The queue is AWS SQS, the database is SQL Server, and the CI/CD pipeline runs tests on every commit via GitHub Actions. The test suite has 147 tests — 127 unit tests and 20 integration tests against a real SQL Server database spun up in Docker."

---

## Advanced Technical Walkthrough

**Target audience**: Senior engineers, tech leads, engineering managers, interviewers.

### Architecture Overview

Start by drawing attention to the four-schema database design — this is the most opinionated architectural decision in the project.

```
staging schema      — raw JSON exactly as received, never modified
validated schema    — records that passed all business rules
final schema        — production data, upserted via MERGE stored procedure
audit schema        — sync_jobs and sync_errors tables, immutable log
```

> "Most ETL systems go straight from source to destination. The four-schema design is deliberate. Staging preserves the raw payload so you can replay transformations if the logic changes. Validated is a quality checkpoint. The final schema is optimized for reads. The audit schema is append-only — it's the paper trail for compliance and debugging."

### The ETL Pipeline — Walk the Code

Show the pipeline for a CRM sync. Each stage is a separate Spring service:

**1. Fetch** — `CrmApiClient.java`
- RestTemplate with 5s connect / 10s read timeouts
- Paginated fetch: loops through all pages until exhausted
- Retry: 3 attempts, exponential backoff, logs each retry

**2. Stage** — `CustomerIntegrationService.java`
- Creates a `SyncJob` record in `audit.sync_jobs` with status QUEUED
- Inserts each raw API response as JSON into `staging.raw_customers`
- Per-record try/catch: one bad record doesn't abort the job

**3. Transform** — `CustomerTransformationService.java`
- Deserializes JSON via Jackson `ObjectMapper`
- Phone normalization: strips all non-digits
- Email: lowercase + trim
- Address: flattens nested `{ street, city, state, zip }` object to a single string

**4. Validate** — `CustomerValidationService.java`
- Stateless — no database calls, pure logic
- Checks: `external_id` required, `name` required, email format (regex if present)
- Collects **all** errors, not just the first — returns `ValidationResult` with a list

**5. Load** — `CustomerLoadService.java`
- Upserts into `validated.validated_customers` (find-by-external-id or create)
- Calls `EXEC [final].upsert_customers` — a SQL Server MERGE stored procedure

Point to `CustomerPipelineService.java` as the orchestrator that wires all five stages together and records errors back to `audit.sync_errors`.

### Async Processing with SQS

> "The REST endpoint doesn't run the pipeline synchronously. It puts a message on a queue and returns 202 Accepted immediately."

Walk through the flow:

1. `POST /api/integrations/sync/customers` → `SyncJobService.createQueuedJob()` → `SyncMessageProducer.send()`
2. Returns HTTP 202 with the QUEUED job
3. `SyncMessageConsumer` (annotated `@SqsListener`) picks up the message
4. Calls `SyncJobService.startJob()` → status: RUNNING → publishes event
5. Runs `CustomerPipelineService.runPipelineForJob(jobId)`
6. On completion: `SyncJobService.completeJob()` or `failJob()` → publishes event

> "In dev, LocalStack is an in-process SQS server. The queue and DLQ are created automatically at startup via an init script. The dead-letter queue captures messages that fail processing three times — nothing gets silently dropped."

Show `SqsConfig.java` — the queue creation logic and redrive policy are in code, not hand-configured.

### GraphQL and Real-Time Subscriptions

Open http://localhost:8080/graphiql and run a live query:

```graphql
query {
  syncJobs(limit: 5, orderBy: START_TIME_DESC) {
    id
    sourceName
    status
    recordsProcessed
    recordsFailed
    duration
    successRate
    errors(limit: 3) {
      errorType
      errorMessage
    }
  }
}
```

> "This is one request. The equivalent in REST would be one call to list jobs, then a second call to `/jobs/{id}/errors` for each job. GraphQL lets the client declare exactly what it needs."

Then show the subscription. Open a second browser tab, trigger a sync from the dashboard, and in GraphiQL run:

```graphql
subscription {
  syncJobUpdated(id: "<paste a job id>") {
    id
    status
    recordsProcessed
    recordsFailed
  }
}
```

> "The frontend uses this same subscription. When a job transitions QUEUED → RUNNING → COMPLETED, the server pushes the update over WebSocket. The dashboard reflects it without polling."

Point to `SyncJobEventPublisher.java` — it's a Reactor `Sinks.Many` that receives events from the service layer and fans them out to any active subscriptions.

Show the DateTime resolver pattern in `SyncJobQueryResolver.java`:
> "JPA entities use `LocalDateTime` for SQL Server compatibility. GraphQL's `DateTime` scalar requires `OffsetDateTime`. Rather than change the entity, field resolvers do the conversion at the boundary — this is a clean separation between persistence and API layers."

### Testing Strategy

> "There are two completely separate test suites with different tooling."

**Unit tests — `./mvnw test`** (127 tests, ~15 seconds, no Docker):
- H2 in-memory database
- All external dependencies mocked with Mockito
- Test every service in isolation: transformation logic, validation rules, error handling paths, event publishing

**Integration tests — `./mvnw failsafe:integration-test failsafe:verify`** (20 tests, ~60–90 seconds, requires Docker):
- Testcontainers 2.0.2 spins up a real Azure SQL Edge container
- Flyway runs all migrations against it — same schema as production
- WireMock stubs the external APIs to return controlled payloads
- Tests verify data ends up in all four schemas correctly

Show `BaseIntegrationTest.java`:
- Container is `static` — shared across all test classes, started once
- `@BeforeEach` truncates all schemas between tests for isolation
- `@DynamicPropertySource` injects the container's dynamic JDBC URL into Spring's context

Show `WireMockStubs.java`:
> "This is where we define what the mock APIs return during integration tests. We can return exactly 3 customers, one of which has a deliberately invalid email, and assert that exactly 2 records land in the final schema and 1 error lands in audit."

Show the Maven plugin split:
```xml
<!-- maven-surefire-plugin excludes *IntegrationTest.java -->
<!-- maven-failsafe-plugin includes *IntegrationTest.java -->
```
> "This means CI runs unit tests on every push — fast, no Docker required. Integration tests run separately. This is intentional: you want fast feedback on the unit tests and don't want them blocked by Docker availability."

### CI/CD Pipeline

Open `.github/workflows/ci.yml` and walk through the three jobs:

```
backend-unit-tests         →  ./mvnw test
backend-integration-tests  →  ./mvnw failsafe:integration-test failsafe:verify
frontend                   →  npm run lint && npx tsc --noEmit && npm run build
```

> "All three run in parallel. The integration test job uses the `ubuntu-latest` runner, which has Docker available — that's what Testcontainers needs. The frontend job runs type-checking separately from the build so TypeScript errors surface clearly in the CI output."

### Design Decisions Worth Discussing

These are good answers to likely interview follow-ups:

**"Why SQL Server instead of Postgres?"**
> Financial services and enterprise orgs are heavily SQL Server shops. It also lets me demonstrate MERGE syntax, schema separation, and SQL Server-specific stored procedures — patterns you see in real enterprise ETL systems.

**"Why not use Spring Batch?"**
> Spring Batch is the right answer for file-based, checkpoint-resumable ETL jobs. This project is API-to-database streaming where per-record error isolation and real-time visibility matter more than checkpoint/restart. The architecture is closer to a microservices data pipeline than a batch processor.

**"Why both REST and GraphQL?"**
> REST for actions (trigger sync, cancel sync) — simple, stateless, cacheable. GraphQL for reads — the dashboard needs nested data (jobs with errors with staging records) and computed fields (duration, successRate, validationStats). WebSocket subscriptions only make sense in GraphQL's subscription model. Hybrid is the practical answer in most real systems.

**"Why Testcontainers instead of mocking the database?"**
> The most critical bugs in data pipelines live at the database boundary — MERGE behavior, constraint violations, Flyway migration ordering, JPA N+1 queries. Mocking the database hides all of that. Testcontainers gives you a real database in CI at the cost of ~60 seconds.

---

## Common Questions and Answers

**"What would you add next?"**
- S3 file ingestion pipeline (CSV/Excel uploads processed via the same ETL stages)
- Role-based access control (Spring Security + JWT) for multi-tenant use
- Playwright E2E tests for the dashboard
- Metrics and alerting (Micrometer + Prometheus + Grafana)

**"How would this scale?"**
- Multiple SQS consumers can run in parallel — Spring Cloud AWS handles competing consumers
- The pipeline services are stateless — horizontal scaling is straightforward
- The MERGE stored procedure is idempotent — safe to re-run on the same data

**"What was the hardest part to build?"**
- The Testcontainers + Docker Desktop compatibility issue (Docker Desktop 4.52+ raised the minimum API version, which broke Testcontainers 1.x — required upgrading to 2.0.2)
- The GraphQL DateTime serialization mismatch between JPA's `LocalDateTime` and the `DateTime` scalar's requirement for `OffsetDateTime`
- Getting the SQS consumer to integrate cleanly with Spring's test context isolation (`@ConditionalOnProperty` to disable SQS in unit tests)
