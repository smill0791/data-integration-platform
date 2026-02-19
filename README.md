# Multi-Source Data Integration Platform

An enterprise-grade data integration system built with Spring Boot, SQL Server, React, and AWS. This project demonstrates proficiency in modern full-stack development and real-world integration patterns used in financial services and enterprise organizations.

## 🎯 Project Overview

The platform ingests data from multiple external APIs (CRM, ERP, Accounting), processes it through a validated ETL pipeline, and surfaces real-time monitoring through a React dashboard backed by both REST and GraphQL APIs.

**Core Skills Demonstrated**:
- Multi-source ETL pipeline with per-record error isolation
- Asynchronous processing via AWS SQS (LocalStack)
- Real-time WebSocket subscriptions with GraphQL
- Four-schema database architecture (staging → validated → final + audit)
- 147 tests: 127 unit (H2) + 20 integration (Testcontainers + real SQL Server)
- CI/CD with GitHub Actions

## 🏗️ Architecture

```
External APIs (CRM / ERP / Accounting)
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
│       │   │   ├── integration/    # External API clients (CRM, ERP, Accounting)
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
│           │   ├── (unit tests)    # 127 tests, H2 in-memory DB
│           │   └── integration/    # 20 tests, Testcontainers SQL Server
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
├── docker-compose.yml             # SQL Server + LocalStack
├── start-dev.sh                   # One-command dev startup
├── CLAUDE.md                      # Development conventions and architecture
└── PROJECT_SPEC.md               # Complete technical specification
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
| JUnit 5 + Mockito | Unit testing (127 tests) |
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
| `GET` | `/api/integrations/jobs` | List recent sync jobs |
| `GET` | `/api/integrations/jobs/{id}` | Get sync job by ID |
| `GET` | `/api/integrations/jobs/{id}/errors` | Get errors for a job |

### GraphQL (`/graphql`)
- **Queries**: `syncJob`, `syncJobs` (filter/sort/paginate), `syncMetrics`
- **Mutations**: `triggerSync`, `cancelSync`
- **Subscriptions**: `syncJobUpdated` (WebSocket, real-time status updates)
- **Playground**: http://localhost:8080/graphiql

## 🧪 Testing

```bash
# Unit tests — 127 tests, H2 in-memory DB, no Docker needed
cd backend && ./mvnw test

# Integration tests — 20 tests, requires Docker (SQL Server + WireMock)
cd backend && ./mvnw failsafe:integration-test failsafe:verify

# Unit + integration together
cd backend && ./mvnw verify

# Frontend
cd frontend && npm run lint && npm run build
```

### Test Coverage Breakdown

| Suite | Tests | Database | External APIs |
|---|---|---|---|
| Service unit tests | 65 | H2 in-memory | Mocked (Mockito) |
| Resolver/controller unit tests | 62 | H2 in-memory | Mocked (Mockito) |
| Pipeline integration tests | 11 | SQL Server (Testcontainers) | WireMock stubs |
| Job lifecycle integration tests | 4 | SQL Server (Testcontainers) | — |
| REST API integration tests | 5 | SQL Server (Testcontainers) | WireMock stubs |

## 📈 Implementation Status

All 8 phases complete:

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
