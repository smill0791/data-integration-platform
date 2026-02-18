# Multi-Source Data Integration Platform

## Project Overview

This is a comprehensive enterprise data integration platform built to demonstrate proficiency in modern full-stack development. The system simulates real-world data integration challenges faced by financial services and enterprise organizations.

**Purpose**: Portfolio project showcasing Spring Boot, SQL Server, React, AWS integration, and enterprise architecture patterns.

**Full Specification**: See [PROJECT_SPEC.md](./PROJECT_SPEC.md) for complete technical details, implementation phases, and architecture diagrams.

## Technology Stack

### Backend
- **Java 17+** with Spring Boot 3.x
- **Spring Data JPA** for ORM with Hibernate
- **Spring Security** for authentication and authorization
- **Spring Web** for REST APIs
- **AWS SDK** for SQS and S3 integration
- **Lombok** to reduce boilerplate
- **Flyway** for database migrations
- **JUnit 5 + Mockito** for testing

### Database
- **SQL Server 2019+** (containerized via Docker)
- **Four-schema architecture**:
  - `staging` - Raw data from external sources (JSON payloads)
  - `validated` - Data that passed validation rules
  - `final` - Production-ready data for reporting
  - `audit` - Sync job tracking, error logs, data lineage

### Frontend
- **React 18** with TypeScript
- **Next.js 14** (App Router)
- **TailwindCSS** for styling
- **React Query** for state management and data fetching
- **Recharts** for data visualization
- **Apollo Client** (optional GraphQL extension)

### Infrastructure
- **Docker** and **Docker Compose** for local development
- **LocalStack** for AWS service mocking (SQS, S3)
- **GitHub Actions** for CI/CD (future)

## Architecture Overview

```
External APIs → Integration Service → SQS Queue → Async Processing
                                         ↓
                            Staging Schema (raw JSON)
                                         ↓
                            Transformation & Validation
                                         ↓
                            Validated Schema (clean data)
                                         ↓
                            Load Service (MERGE/upsert)
                                         ↓
                            Final Schema (production data)

                            Audit Schema (tracks everything)
                                         ↓
                            React Dashboard (monitoring & metrics)
```

### Data Flow Principles

1. **Extract**: Fetch data from external APIs (CRM, ERP, Accounting)
2. **Stage**: Store raw JSON payloads in `staging` schema with sync_job_id linkage
3. **Transform**: Parse, clean, normalize data (phone numbers, emails, addresses)
4. **Validate**: Apply business rules, check required fields, validate formats
5. **Load**: MERGE into `final` schema (INSERT new, UPDATE existing by external_id)
6. **Audit**: Track all sync operations, errors, and data lineage

## Code Conventions & Patterns

### Backend (Spring Boot)

**Package Structure**:
- `config/` - Spring configuration classes (AWS, Security, Database)
- `controller/` - REST API endpoints (use `@RestController`, return `ResponseEntity`)
- `service/` - Business logic layer (use `@Service`, `@Transactional`)
- `repository/` - Data access layer (extend `JpaRepository`)
- `model/` - JPA entities (use `@Entity`, map to database tables)
- `dto/` - Data Transfer Objects for API requests/responses
- `integration/` - External API clients (use `RestTemplate` or `WebClient`)
- `transformer/` - Data transformation logic
- `validator/` - Custom validation rules
- `exception/` - Custom exceptions and global exception handlers

**Standards**:
- Use **constructor injection**, NOT field injection (`@RequiredArgsConstructor` from Lombok)
- Use **SLF4J** for logging: `@Slf4j` annotation, then `log.info()`, `log.error()`
- All controller methods return `ResponseEntity<T>` for consistent HTTP responses
- Use `@Transactional` on service methods that modify data
- Validation errors return **400 Bad Request** with detailed error messages
- Follow REST conventions: GET (read), POST (create), PUT (update), DELETE (remove)
- Store credentials in `application.yml` with environment variable placeholders

### Frontend (React/Next.js)

**Component Structure**:
- `app/` - Next.js app router pages and layouts
- `components/` - Reusable React components (PascalCase naming)
- `hooks/` - Custom React hooks (prefix with `use`, e.g., `useSyncJobs`)
- `services/` - API client functions (e.g., `syncJobService.ts`)
- `types/` - TypeScript type definitions and interfaces

**Standards**:
- Use **TypeScript** strictly, avoid `any` types
- Components are **function components** with hooks
- Use **React Query** for server state (caching, refetching, mutations)
- Use **TailwindCSS** utility classes for styling
- Follow atomic design: atoms → molecules → organisms → pages
- Handle loading states, error states, and empty states gracefully

### GraphQL (Optional Extension)

**Backend Structure** (`backend/src/main/java/com/dataplatform/graphql/`):
- `SyncJobResolver.java` - GraphQL resolvers for queries, mutations, subscriptions
- Schema definition: `backend/src/main/resources/graphql/schema.graphqls`

**Frontend Structure**:
- `lib/apollo-client.ts` - Apollo Client configuration
- `graphql/queries/` - GraphQL query definitions
- `graphql/mutations/` - GraphQL mutation definitions
- `graphql/subscriptions/` - GraphQL subscription definitions (WebSocket)

**GraphQL Patterns**:
- Use **fragments** for reusable field selections (`SyncJobCoreFields`)
- Queries for reading data: `useQuery(GET_SYNC_JOBS)`
- Mutations for writing data: `useMutation(TRIGGER_SYNC)`
- Subscriptions for real-time updates: `useSubscription(WATCH_SYNC_JOB)`
- Configure cache policies in Apollo Client for optimal performance

**When to Use**:
- ✅ Dashboard with complex nested data from multiple sources
- ✅ Real-time sync monitoring with WebSocket subscriptions
- ✅ Flexible filtering and field selection
- ❌ File uploads (use REST)
- ❌ Simple CRUD operations (use REST)

**GraphQL vs REST**:
- **REST**: Simple operations, file uploads, webhooks, health checks
- **GraphQL**: Complex queries, real-time updates, flexible data fetching
- **Hybrid Approach**: Use both - REST for actions, GraphQL for queries

**Dependencies** (Backend - Maven):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**Dependencies** (Frontend - npm):
```bash
npm install @apollo/client graphql graphql-ws
```

**Testing GraphQL**:
- Access GraphiQL at `http://localhost:8080/graphiql`
- Use `@AutoConfigureGraphQlTester` for Spring Boot tests
- Mock Apollo Client with `MockedProvider` in React tests

### Database

**Schema Organization**:
```sql
-- Audit tables track all sync activity
CREATE TABLE audit.sync_jobs (...);
CREATE TABLE audit.sync_errors (...);

-- Staging holds raw data exactly as received
CREATE TABLE staging.raw_customers (...);
CREATE TABLE staging.raw_orders (...);

-- Validated holds cleaned, validated data
CREATE TABLE validated.validated_customers (...);
CREATE TABLE validated.validated_orders (...);

-- Final holds production data with upsert logic
CREATE TABLE final.customers (...);
CREATE TABLE final.orders (...);
```

**Flyway Migrations**:
- Files go in `backend/src/main/resources/db/migration/`
- Naming: `V1__initial_schema.sql`, `V2__add_orders_table.sql`
- Never modify existing migrations, always create new ones
- Use `CREATE INDEX` for columns used in WHERE clauses and JOINs

**Stored Procedures**:
- Use for bulk operations (MERGE upsert logic)
- Example: `final.upsert_customers` procedure

## Development Workflow

### Local Environment Setup

1. **Start infrastructure**:
   ```bash
   docker-compose up -d
   ```
   This starts SQL Server (port 1433) and LocalStack (port 4566)

2. **Run backend**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   Backend runs on http://localhost:8080

3. **Run mock API**:
   ```bash
   cd mock-apis/crm-api
   npm install && npm start
   ```
   Mock CRM API runs on http://localhost:3001

4. **Run frontend**:
   ```bash
   cd frontend
   npm install && npm run dev
   ```
   Frontend runs on http://localhost:3000

### Database Connection
- **Host**: localhost:1433
- **Username**: sa
- **Password**: YourStrong@Passw0rd
- **Tool**: Azure Data Studio, DBeaver, or SQL Server Management Studio

### Testing Strategy

**Unit Tests** (JUnit + Mockito):
- Test service layer methods in isolation
- Mock all dependencies (repositories, API clients)
- Target >80% code coverage
- Run: `./mvnw test`

**Integration Tests** (Spring Boot Test + Testcontainers):
- Test with real database (SQL Server container)
- Test full sync flow: fetch → transform → validate → load
- Verify data in all schemas

**E2E Tests** (Playwright):
- Test complete user workflows through UI
- Trigger sync, verify status updates, check job details

## Current Implementation Status

**Phase 1: Backend Foundation** (Complete)
- [x] Git repository initialized
- [x] Project structure created (backend, frontend, mock-apis, database)
- [x] CLAUDE.md created with development conventions
- [x] Docker Compose configured (SQL Server + LocalStack)
- [x] .gitignore files added (root, backend, frontend)
- [x] GraphQL baseline structure created
  - [x] Backend GraphQL schema and package structure
  - [x] Frontend Apollo Client setup and query templates
  - [x] GraphQL documentation and patterns
- [x] GitHub repository created and pushed
- [x] Spring Boot 3.2 backend initialized with dependencies
  - [x] Maven build (pom.xml) with Spring Web, Data JPA, Security, Validation, DevTools
  - [x] GraphQL + WebFlux starters with extended scalars (DateTime, Date)
  - [x] SQL Server driver, Flyway, Lombok, H2 (test)
  - [x] Maven wrapper (mvnw) for portable builds
- [x] Application configuration
  - [x] application.yml with SQL Server datasource, JPA, Flyway, GraphQL settings
  - [x] application-test.yml with H2 in-memory test profile
  - [x] Spring Security config (CORS for localhost:3000, permit all for now)
  - [x] GraphQL scalar config (DateTime, Date)
- [x] Domain models and repositories
  - [x] SyncJob entity (audit.sync_jobs)
  - [x] SyncError entity (audit.sync_errors)
  - [x] RawCustomer entity (staging.raw_customers)
  - [x] JPA repositories with query methods for each entity
- [x] Database migrations created (Flyway)
  - [x] V1__initial_schema.sql: 4 schemas (audit, staging, validated, final) and 5 tables
  - [x] Database init script for Docker (01-create-database.sql)
- [x] Health check endpoint (GET /health)
- [x] Context load test passing (DataIntegrationApplicationTests)
- [x] Docker Compose health check fixed (mssql-tools18 path)

**Phase 2: CRM API Integration Pipeline** (Complete)
- [x] Mock CRM API (Express.js)
  - [x] 100 fake customers generated at startup via @faker-js/faker
  - [x] `GET /api/customers?page=0&size=20` — paginated response
  - [x] `GET /api/customers/:id` — single customer lookup
  - [x] Random delay (100-500ms) and 5% simulated failure rate
- [x] DTOs
  - [x] `CrmCustomerResponse` — CRM customer with nested Address
  - [x] `PaginatedResponse<T>` — generic paginated wrapper
  - [x] `SyncJobDTO` — API response DTO with `fromEntity()` converter
- [x] Exception handling
  - [x] `IntegrationException` — upstream API failures (maps to 502)
  - [x] `ResourceNotFoundException` — missing entities (maps to 404)
  - [x] `GlobalExceptionHandler` — @RestControllerAdvice with structured error responses
- [x] REST client infrastructure
  - [x] `RestTemplateConfig` — RestTemplate bean with 5s connect / 10s read timeouts
  - [x] `CrmApiClient` — paginated fetch, retry logic (3 attempts, exponential backoff)
  - [x] CRM integration properties in application.yml (`integration.crm.*`)
- [x] Service layer
  - [x] `SyncJobService` — sync job lifecycle (create, complete, fail) with individual @Transactional boundaries
  - [x] `CustomerIntegrationService` — orchestrates fetch → stage → audit with per-record error isolation
- [x] REST controller
  - [x] `POST /api/integrations/sync/customers` — trigger customer sync
  - [x] `GET /api/integrations/jobs` — list recent sync jobs
  - [x] `GET /api/integrations/jobs/{id}` — get sync job by ID
- [x] Unit tests (12/12 passing)
  - [x] `SyncJobServiceTest` — 7 tests (create, complete, fail, get, not-found, recent)
  - [x] `CustomerIntegrationServiceTest` — 4 tests (success, partial failure, API failure, empty response)
- [x] Build fixes
  - [x] Lombok annotation processor configured in maven-compiler-plugin
  - [x] byte-buddy upgraded to 1.15.11 for Java 23 compatibility

**Phase 3: Transformation & Loading Pipeline** (Complete)
- [x] Flyway migration V2 — `[final].upsert_customers` stored procedure (SQL Server MERGE: update on match, insert on no match)
- [x] DTOs
  - [x] `TransformedCustomer` — intermediate DTO with cleaned fields (externalId, name, email, phone, flattened address, rawData)
  - [x] `ValidationResult` — wraps valid boolean, TransformedCustomer, and List<String> errors
- [x] Entities and repositories
  - [x] `ValidatedCustomer` entity (validated.validated_customers) + `ValidatedCustomerRepository` with `findByExternalId()`
  - [x] `FinalCustomer` entity ([final].customers) + `FinalCustomerRepository` with `findByExternalId()`
- [x] `CustomerTransformationService` — parses raw JSON via ObjectMapper, normalizes phone (strip non-digits), email (lowercase+trim), address (flatten to "street, city, state zip"), name (trim)
- [x] `CustomerValidationService` — stateless validation: external_id required, name required, email format regex (if present), returns all errors
- [x] `CustomerLoadService` — @Transactional upsert into validated schema (find-or-create by external_id) + EXEC stored procedure for final schema with source_system='CRM'
- [x] `CustomerPipelineService` — full pipeline orchestrator: stage → transform → validate → load with per-record error isolation (VALIDATION_ERROR / PIPELINE_ERROR)
- [x] `SyncJobService.getJobEntity()` — returns raw SyncJob entity for pipeline orchestrator
- [x] `IntegrationController` updated — POST /api/integrations/sync/customers now runs full pipeline via CustomerPipelineService
- [x] Unit tests (24 new, 36 total passing)
  - [x] `CustomerTransformationServiceTest` — 8 tests (happy path, null fields, partial address, invalid JSON, mixed-case email, phone formats)
  - [x] `CustomerValidationServiceTest` — 7 tests (valid, missing fields, invalid email, null email, multiple errors, edge-case emails)
  - [x] `CustomerLoadServiceTest` — 3 tests (new record, existing record update, JDBC failure propagation)
  - [x] `CustomerPipelineServiceTest` — 6 tests (all success, staging fails, validation/transform/load failures, empty staging)

**Phase 4: React Dashboard** (Complete)
- [x] Backend sync errors endpoint
  - [x] `SyncErrorDTO` — DTO with `fromEntity()` mapping
  - [x] `SyncJobService.getErrorsForJob()` — validates job exists (404), returns mapped errors
  - [x] `GET /api/integrations/jobs/{id}/errors` — new controller endpoint
  - [x] Unit tests (2 new, 38 total passing)
- [x] Frontend infrastructure
  - [x] `next.config.js`, `tsconfig.json`, `tailwind.config.js`, `postcss.config.js`
  - [x] `.env.local.example` documenting `NEXT_PUBLIC_API_URL`
  - [x] `globals.css` (Tailwind directives), `providers.tsx` (React Query + Apollo), `layout.tsx`
- [x] Data layer
  - [x] `types/syncJob.ts` — SyncJob, SyncError, DashboardMetrics, DailyStatPoint interfaces
  - [x] `services/syncJobService.ts` — fetch wrappers for all 4 API calls
- [x] Custom hooks
  - [x] `useSyncJobs` — polls every 5s only when a RUNNING job exists
  - [x] `useSyncJob(id)` — polls every 5s while job is RUNNING
  - [x] `useSyncErrors(jobId)` — no polling (errors are immutable)
  - [x] `useTriggerSync` — mutation with `invalidateQueries` on success
  - [x] `useDashboardMetrics(jobs)` — client-side computation of 24h/30d metrics + daily chart data
- [x] Components
  - [x] `StatusBadge` — colored pill with pulse animation for RUNNING
  - [x] `MetricCard`, `LoadingSpinner`, `ErrorAlert` — shared atoms
  - [x] `SyncJobTable` — sortable table with row links to job detail
  - [x] `TriggerSyncButton` — self-contained with loading and error state
  - [x] `SyncMetricsChart` — Recharts BarChart with completed/failed series
  - [x] `SyncJobDetails` — job summary grid + error log table
- [x] Pages
  - [x] `app/page.tsx` — dashboard with metric cards, chart, job table
  - [x] `app/jobs/[id]/page.tsx` — job detail with auto-polling while RUNNING
- [x] `start-dev.sh` — single script to start backend, mock API, and frontend together

**Future Phases**:
- **Phase 5**: GraphQL resolvers for real-time dashboard updates (subscriptions)
- **Phase 6**: SQS async processing via LocalStack — decouple sync triggers from processing
- **Phase 7**: Additional data sources (ERP, Accounting mock APIs)
- **Phase 8**: CI/CD with GitHub Actions, integration tests with Testcontainers

## Key Design Decisions

### Why Four Schemas?
- **Staging**: Preserves raw data for replay if transformation logic changes
- **Validated**: Intermediate checkpoint for data quality review
- **Final**: Optimized for queries, contains production data
- **Audit**: Permanent records for compliance and debugging

### Why SQS?
- **Decoupling**: Sync requests can be queued even if workers are busy
- **Scalability**: Multiple workers can process queue in parallel
- **Reliability**: Built-in retry logic and dead-letter queues
- **LocalStack**: Test AWS integration without cloud costs

### Why Next.js?
- **Server Components**: Improved performance with React Server Components
- **API Routes**: Can serve backend APIs if needed
- **TypeScript**: Built-in support without extra configuration
- **Production-Ready**: Image optimization, routing, SEO built-in

## Common Commands

### Docker
```bash
docker-compose up -d              # Start all services
docker-compose down               # Stop all services
docker-compose logs -f sqlserver  # View SQL Server logs
docker-compose ps                 # Check running services
```

### Backend (Maven)
```bash
./mvnw spring-boot:run           # Run Spring Boot app
./mvnw test                      # Run all tests
./mvnw clean package             # Build JAR file
./mvnw flyway:migrate            # Run database migrations
./mvnw flyway:info               # Check migration status
```

### Frontend (npm)
```bash
npm run dev                      # Start dev server
npm run build                    # Build for production
npm test                         # Run Jest tests
npm run lint                     # Run ESLint
```

### Git
```bash
git status                       # Check repository status
git add .                        # Stage all changes
git commit -m "message"          # Commit changes
git push origin main             # Push to GitHub
```

## Troubleshooting

### SQL Server won't start
- Check Docker Desktop is running
- Ensure port 1433 is not in use: `lsof -i :1433`
- Check logs: `docker-compose logs sqlserver`
- Try stronger password in docker-compose.yml

### Backend can't connect to database
- Verify SQL Server container is running: `docker ps`
- Check `application.yml` has correct connection string
- Test connection manually with Azure Data Studio
- Check Flyway migrations completed successfully

### Mock API returns 404
- Ensure Express server is running on port 3001
- Check if port is already in use: `lsof -i :3001`
- Verify routes are registered: check `mock-apis/crm-api/server.js`

### Frontend can't reach backend
- Verify backend is running on port 8080
- Check CORS configuration in Spring Security
- Inspect browser Network tab for errors
- Ensure API base URL is correct in frontend service

## Resources & References

- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **SQL Server Docker**: https://hub.docker.com/_/microsoft-mssql-server
- **Next.js Docs**: https://nextjs.org/docs
- **React Query**: https://tanstack.com/query/latest
- **LocalStack**: https://docs.localstack.cloud/
- **Flyway**: https://flywaydb.org/documentation/

## Notes

This project is designed to be portfolio-ready and demonstrates skills required for enterprise full-stack development roles. It showcases real-world integration patterns, data quality management, and modern development practices.

For complete implementation guide with day-by-day tasks and Cursor AI prompts, see [PROJECT_SPEC.md](./PROJECT_SPEC.md).
