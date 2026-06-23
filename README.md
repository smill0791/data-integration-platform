# Multi-Source Data Integration Platform

An enterprise-grade data integration system built with Spring Boot, SQL Server, React, and AWS. This project demonstrates proficiency in modern full-stack development and real-world integration patterns used in financial services and enterprise organizations.

## 🎯 Project Overview

The platform ingests data from multiple external APIs (CRM, ERP, Accounting, Salesforce), processes it through a validated ETL pipeline, and surfaces real-time monitoring through a React dashboard backed by both REST and GraphQL APIs.

**Core Skills Demonstrated**:
- Multi-source ETL pipeline with per-record error isolation
- Asynchronous processing via AWS SQS (LocalStack)
- Real-time WebSocket subscriptions with GraphQL
- Four-schema database architecture (staging → validated → final + audit)
- Salesforce OAuth integration + Lightning Web Component
- 168 tests: 144 unit (H2) + 24 integration (Testcontainers + real SQL Server)
- CI/CD with GitHub Actions

## 🏗️ Architecture

```
External APIs (CRM / ERP / Accounting / Salesforce)
        ↓
  Integration Service
        ↓
  SQS Queue (async)          ← LocalStack in dev
        ↓
  Staging Schema             ← raw JSON, preserved for replay
        ↓
  Transform + Validate       ← normalize, clean, check business rules
        ↓
  Validated Schema           ← checkpoint for data quality review
        ↓
  Load (MERGE / upsert)      ← stored procedures, idempotent
        ↓
  Final Schema               ← production-ready data

  Audit Schema               ← all sync jobs, errors, data lineage
        ↓
  React Dashboard            ← REST + GraphQL + WebSocket subscriptions
```

## 🚀 Quick Start

### Prerequisites

- Java 17+ JDK
- Node.js 18+
- Docker Desktop
- Maven 3.8+

### Start Everything

```bash
# 1. Start SQL Server and LocalStack
docker-compose up -d

# 2. Start all services with a single script
./start-dev.sh
```

`start-dev.sh` starts and manages all five services:

| Service | Port | Description |
|---|---|---|
| React Dashboard | 3000 | Next.js frontend |
| Mock CRM API | 3001 | 100 fake customers |
| Mock ERP API | 3002 | 80 fake products |
| Mock Accounting API | 3003 | 60 fake invoices |
| Spring Boot Backend | 8080 | REST + GraphQL |

Press `Ctrl+C` to stop all services. The script auto-kills stale processes on startup.

### Or Start Services Individually

```bash
cd backend && ./mvnw spring-boot:run       # http://localhost:8080
cd mock-apis/crm-api && npm start          # http://localhost:3001
cd mock-apis/erp-api && npm start          # http://localhost:3002
cd mock-apis/accounting-api && npm start   # http://localhost:3003
cd frontend && npm run dev                 # http://localhost:3000
```

## 📁 Project Structure

```
data-integration-platform/
├── .github/workflows/         # GitHub Actions CI/CD
│   └── ci.yml                 # Unit tests, integration tests, frontend build
├── backend/
│   └── src/
│       ├── main/
│       │   ├── java/com/dataplatform/
│       │   │   ├── config/         # Spring Security, RestTemplate, AWS
│       │   │   ├── controller/     # REST endpoints
│       │   │   ├── dto/            # API request/response objects
│       │   │   ├── exception/      # Global exception handling
│       │   │   ├── graphql/        # Query, mutation, subscription resolvers
│       │   │   ├── integration/    # External API clients (CRM, ERP, Accounting, Salesforce)
│       │   │   ├── model/          # JPA entities (all 4 schemas)
│       │   │   ├── repository/     # Spring Data JPA repositories
│       │   │   ├── service/        # Business logic, pipeline orchestration
│       │   │   ├── sqs/            # SQS producer and consumer
│       │   │   ├── transformer/    # Data normalization
│       │   │   └── validator/      # Business rule validation
│       │   └── resources/
│       │       ├── db/migration/   # Flyway migrations (V1–V4)
│       │       └── graphql/        # GraphQL schema definition
│       └── test/
│           ├── java/com/dataplatform/
│           │   ├── (unit tests)    # 144 tests, H2 in-memory DB
│           │   └── integration/    # 24 tests, Testcontainers SQL Server
│           └── resources/          # application-integration-test.yml
├── frontend/
│   └── src/
│       ├── app/                    # Next.js App Router pages
│       ├── components/             # Reusable React components
│       ├── graphql/                # Apollo queries, mutations, subscriptions
│       ├── hooks/                  # Custom React hooks (REST + GraphQL)
│       ├── services/               # REST API client functions
│       └── types/                  # TypeScript type definitions
├── mock-apis/
│   ├── crm-api/                    # Express, @faker-js/faker, port 3001
│   ├── erp-api/                    # Express, @faker-js/faker, port 3002
│   └── accounting-api/             # Express, @faker-js/faker, port 3003
├── localstack/
│   └── init-scripts/              # SQS queue creation on startup
├── salesforce-lwc/                # SFDX project: Apex controller + syncDashboard LWC
├── docs/                          # Specification, codebase guide, setup, demo notes
│   ├── PROJECT_SPEC.md            # Complete technical specification
│   ├── CODEBASE_GUIDE.md          # Architecture deep-dive
│   ├── salesforce-setup.md        # Salesforce Dev Org + OAuth setup
│   └── DEMO_GUIDE.md              # Demo walkthrough notes
├── docker-compose.yml             # SQL Server + LocalStack
├── start-dev.sh                   # One-command dev startup
├── CLAUDE.md                      # Development conventions and architecture
└── README.md                      # This file
```

## 🛠️ Technology Stack

### Backend
| Technology | Purpose |
|---|---|
| Spring Boot 3.2 | Application framework |
| Spring Data JPA / Hibernate | ORM and data access |
| Spring Security | CORS and auth configuration |
| Spring for GraphQL + WebFlux | GraphQL resolvers and WebSocket subscriptions |
| Flyway | Database migrations (V1–V4) |
| AWS SDK / Spring Cloud AWS | SQS producer and consumer |
| Lombok | Reduce boilerplate |
| JUnit 5 + Mockito | Unit testing (144 tests) |
| Testcontainers 2.0.2 | Integration testing with real SQL Server |
| WireMock | HTTP API stubbing in integration tests |

### Database
| Technology | Purpose |
|---|---|
| SQL Server (Azure SQL Edge in tests) | Primary database |
| Four-schema architecture | staging, validated, final, audit |
| Stored procedures (MERGE) | Idempotent upsert for final schema |
| Flyway migrations | Schema version control |

### Frontend
| Technology | Purpose |
|---|---|
| React 18 + TypeScript | UI library |
| Next.js 14 (App Router) | React framework with SSR |
| TailwindCSS | Utility-first styling |
| React Query (TanStack) | REST data fetching and caching |
| Apollo Client | GraphQL queries, mutations, subscriptions |
| Recharts | Sync metrics bar chart |

### Infrastructure
| Technology | Purpose |
|---|---|
| Docker + Docker Compose | SQL Server + LocalStack containers |
| LocalStack | AWS SQS and S3 mocking |
| GitHub Actions | CI/CD pipeline |

## 🔌 API Endpoints

### REST
| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Health check |
| `POST` | `/api/integrations/sync/customers` | Queue CRM sync (returns 202) |
| `POST` | `/api/integrations/sync/products` | Queue ERP sync (returns 202) |
| `POST` | `/api/integrations/sync/invoices` | Queue Accounting sync (returns 202) |
| `POST` | `/api/integrations/sync/salesforce-contacts` | Queue Salesforce contact sync (returns 202) |
| `GET` | `/api/integrations/jobs` | List recent sync jobs |
| `GET` | `/api/integrations/jobs/{id}` | Get sync job by ID |
| `GET` | `/api/integrations/jobs/{id}/errors` | Get errors for a job |

### GraphQL (`/graphql`)
- **Queries**: `syncJob`, `syncJobs` (filter/sort/paginate), `syncMetrics`
- **Mutations**: `triggerSync` (accepts `sourceName`: CRM / ERP / ACCOUNTING / SALESFORCE), `cancelSync`
- **Subscriptions**: `syncJobUpdated` (WebSocket, real-time status updates)
- **Playground**: http://localhost:8080/graphiql

## 🔒 Security & Configuration

**Secrets are never committed.** Every credential is read from an environment
variable with a local-dev fallback (see `backend/src/main/resources/application.yml`,
e.g. `${SF_CLIENT_SECRET:}`, `${DB_PASSWORD:YourStrong@Passw0rd}`). Real Salesforce
credentials live in a gitignored `.env` that is sourced before startup — they are
not present in the repository or its git history. See
[docs/salesforce-setup.md](./docs/salesforce-setup.md) for the required variables.

```bash
# Required for Salesforce sync (set in .env, then `source .env`)
export SF_LOGIN_URL=...        export SF_CLIENT_ID=...
export SF_CLIENT_SECRET=...    export SF_API_VERSION=v59.0
# Optional overrides (sensible local defaults exist for all of these)
export DB_PASSWORD=...         export AWS_ACCESS_KEY=...   export AWS_SECRET_KEY=...
```

**Intentional local-dev scope.** A few choices are deliberately simplified for a
self-contained demo and would change for production:

- **API authentication** — `SecurityConfig` permits all requests so the dashboard
  and mock clients work without a login flow. In production this API would sit
  behind authentication (e.g. a Spring Security OAuth2 resource server / JWT) with
  per-endpoint authorization, rather than `anyRequest().permitAll()`.
- **CORS** is restricted to `localhost:3000` and Salesforce domains
  (`*.lightning.force.com`, `*.my.salesforce.com`), not left fully open.
- **Salesforce callout** — the `syncDashboard` LWC reaches the backend through an
  Apex callout. For a real deployment the endpoint belongs in a **Named Credential**
  (which also stores auth securely), not the hardcoded host used for local tunneling.

## 🧪 Testing

```bash
# Unit tests — 144 tests, H2 in-memory DB, no Docker needed
cd backend && ./mvnw test

# Integration tests — 24 tests, requires Docker (SQL Server + WireMock)
cd backend && ./mvnw failsafe:integration-test failsafe:verify

# Unit + integration together
cd backend && ./mvnw verify

# Frontend
cd frontend && npm run lint && npm run build
```

### Test Coverage Breakdown

| Suite | Tests | Database | External APIs |
|---|---|---|---|
| Service, client, transform & validation unit tests | 126 | H2 in-memory | Mocked (Mockito) |
| GraphQL resolver unit tests | 17 | H2 in-memory | Mocked (Mockito) |
| Application context load | 1 | H2 in-memory | — |
| Pipeline integration tests (CRM / ERP / Accounting / Salesforce) | 15 | SQL Server (Testcontainers) | WireMock stubs |
| Job lifecycle integration tests | 4 | SQL Server (Testcontainers) | — |
| REST API integration tests | 5 | SQL Server (Testcontainers) | WireMock + `@MockBean` |

**Total: 168** — 144 unit (`./mvnw test`) + 24 integration (`./mvnw verify`).

## 📈 Implementation Status

All 9 phases complete:

**Phase 1 — Backend Foundation**
- [x] Spring Boot 3.2 with all dependencies configured
- [x] Four-schema SQL Server database (staging, validated, final, audit)
- [x] Flyway migrations, JPA entities, repositories
- [x] Spring Security (CORS), GraphQL scalars, health endpoint

**Phase 2 — CRM API Integration Pipeline**
- [x] Mock CRM API (Express + Faker, 100 customers, 5% simulated failures)
- [x] `CrmApiClient` with retry logic (3 attempts, exponential backoff)
- [x] `CustomerIntegrationService` — fetch → stage → audit with per-record error isolation
- [x] REST endpoints: trigger sync, list jobs, get job by ID

**Phase 3 — Transformation & Loading Pipeline**
- [x] `CustomerTransformationService` — normalize phone, email, address, name
- [x] `CustomerValidationService` — required fields, email format, collect all errors
- [x] `CustomerLoadService` — upsert to validated schema + MERGE stored procedure for final
- [x] `CustomerPipelineService` — full orchestrator: stage → transform → validate → load

**Phase 4 — React Dashboard**
- [x] Next.js 14 frontend with TailwindCSS
- [x] React Query hooks with smart polling (active only for RUNNING/QUEUED jobs)
- [x] Dashboard: metric cards, sync metrics chart (Recharts), job table
- [x] Job detail page with error log table
- [x] `start-dev.sh` for one-command startup

**Phase 5 — GraphQL with Real-Time Subscriptions**
- [x] GraphQL resolvers: queries (`syncJobs`, `syncMetrics`), mutations (`triggerSync`), subscriptions (`syncJobUpdated`)
- [x] WebSocket subscriptions using Reactor `Sinks.Many` for real-time event streaming
- [x] Frontend migrated to Apollo Client (queries + live subscriptions)
- [x] DateTime field resolvers (LocalDateTime → OffsetDateTime for GraphQL scalars)

**Phase 6 — SQS Async Processing via LocalStack**
- [x] Spring Cloud AWS SQS integration with `@SqsListener`
- [x] `QUEUED` status: sync requests return 202 immediately, processed async
- [x] Dead-letter queue with redrive policy (configured via LocalStack init script)
- [x] SQS disabled in unit tests via `@ConditionalOnProperty`

**Phase 7 — ERP & Accounting Data Sources**
- [x] Mock ERP API (80 products) and Mock Accounting API (60 invoices with line items)
- [x] Full pipelines for products and invoices (transform → validate → load)
- [x] Flyway migrations V3/V4 with `upsert_products` and `upsert_invoices` stored procedures
- [x] Multi-source SQS routing and multi-source GraphQL staging record dispatch
- [x] Frontend: source filter pills, color-coded source badges, SourceSyncPanel

**Phase 8 — CI/CD & Integration Tests**
- [x] GitHub Actions: `backend-unit-tests`, `backend-integration-tests`, `frontend` jobs
- [x] Testcontainers 2.0.2 with Azure SQL Edge (ARM-native, Apple Silicon compatible)
- [x] WireMock for HTTP API stubbing in integration tests
- [x] `BaseIntegrationTest` — shared container lifecycle, schema cleanup between tests
- [x] Maven Surefire/Failsafe split: `./mvnw test` (unit only) vs `./mvnw verify` (all)

**Phase 9 — Salesforce Integration & Lightning Web Component**
- [x] Salesforce OAuth client (`SalesforceAuthService`) with in-memory token caching and refresh
- [x] `SalesforceApiClient` — SOQL contact query, cursor pagination, 401-retry with token refresh
- [x] `SalesforceIntegrationService` — normalizes contacts and reuses the customer pipeline (no new migration)
- [x] `POST /api/integrations/sync/salesforce-contacts` + `triggerSync` with `sourceName=SALESFORCE`
- [x] `syncDashboard` Lightning Web Component + `DataPlatformController` Apex (see `salesforce-lwc/`)

## 🎓 Concepts Demonstrated

1. **ETL Architecture** — Four-schema design preserves raw data, enables replay, enforces data quality gates
2. **Async Processing** — SQS decouples sync requests from execution; DLQ handles poison messages
3. **API Design** — REST for actions, GraphQL for complex queries, WebSocket for real-time updates
4. **Data Quality** — Per-record validation with full error reporting; partial failures don't abort a job
5. **Database Design** — Stored procedures for idempotent MERGE upsert; Flyway for migration versioning
6. **Testing Strategy** — Unit tests with mocks for speed; Testcontainers for realistic integration coverage
7. **CI/CD** — Automated test gates on every push and PR via GitHub Actions

## 🔧 Troubleshooting

**SQL Server won't start**: Check Docker Desktop is running; verify port 1433 is free (`lsof -i :1433`)

**Port conflicts on startup**: `start-dev.sh` auto-kills stale processes on 3000/3001/3002/3003/8080. Manual fix: `lsof -ti :<port> | xargs kill`

**Integration tests failing**: Ensure Docker Desktop is running. Azure SQL Edge image requires ~2GB RAM. Run `docker pull mcr.microsoft.com/azure-sql-edge` to pre-pull.

**GraphQL DateTime errors**: All resolvers convert `LocalDateTime` → `OffsetDateTime` via `atZone(ZoneId.systemDefault()).toOffsetDateTime()`. See `SyncJobQueryResolver` for reference.

See [CLAUDE.md](./CLAUDE.md) for complete troubleshooting guide and architecture decisions.

## 📝 License

Portfolio project for educational and demonstration purposes.

---

**Built to showcase enterprise software engineering skills**
