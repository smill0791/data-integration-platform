# Multi-Source Data Integration Platform

## Project Overview

This is a comprehensive enterprise data integration platform built to demonstrate proficiency in modern full-stack development. The system simulates real-world data integration challenges faced by financial services and enterprise organizations.

**Purpose**: Portfolio project showcasing Spring Boot, SQL Server, React, AWS integration, and enterprise architecture patterns.

**Full Specification**: See [docs/PROJECT_SPEC.md](./docs/PROJECT_SPEC.md) for complete technical details, implementation phases, and architecture diagrams.

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

2. **Start all services** (recommended):
   ```bash
   ./start-dev.sh
   ```
   This starts the Mock CRM API (3001), Spring Boot backend (8080), and Next.js frontend (3000) together. It auto-kills stale processes on those ports. Press Ctrl+C to stop all.

3. **Or start services individually**:
   ```bash
   cd backend && ./mvnw spring-boot:run    # Backend on http://localhost:8080
   cd mock-apis/crm-api && npm start       # Mock CRM API on http://localhost:3001
   cd frontend && npm run dev              # Frontend on http://localhost:3000
   ```

4. **GraphiQL**: http://localhost:8080/graphiql — interactive GraphQL query editor

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

All nine build phases are complete. The platform ingests from four sources
(CRM, ERP, Accounting, Salesforce) through the full staging → validated → final
pipeline, with async SQS processing, REST + GraphQL APIs, a React dashboard, and
a Salesforce Lightning Web Component.

| Phase | Scope | Key artifacts |
|---|---|---|
| 1 | Backend foundation | Spring Boot 3.2, four-schema DB, Flyway V1, health endpoint |
| 2 | CRM integration pipeline | `CrmApiClient`, `CustomerIntegrationService`, sync job lifecycle |
| 3 | Transform & load | Transformation/validation/load services, `upsert_customers` MERGE proc |
| 4 | React dashboard | Next.js 14, React Query hooks, metric cards, job detail + error log |
| 5 | GraphQL + real-time | Query/mutation/subscription resolvers, Apollo client, WebSocket updates |
| 6 | SQS async processing | `@SqsListener`, `QUEUED` status, DLQ via LocalStack |
| 7 | ERP & Accounting sources | Product/invoice pipelines, migrations V3/V4, multi-source routing |
| 8 | CI/CD & integration tests | GitHub Actions, Testcontainers (Azure SQL Edge), WireMock |
| 9 | Salesforce + LWC | OAuth client, contact sync, `syncDashboard` LWC, Apex controller |

Detailed per-phase implementation notes (file-by-file checklists) live in git
history and [docs/PROJECT_SPEC.md](./docs/PROJECT_SPEC.md). Architecture rationale
is captured in [docs/CODEBASE_GUIDE.md](./docs/CODEBASE_GUIDE.md).

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
./mvnw test                      # Run unit tests (144 tests, no Docker needed)
./mvnw verify                    # Run unit + integration tests (requires Docker)
./mvnw failsafe:integration-test failsafe:verify  # Run integration tests only
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

### GraphQL DateTime serialization errors
- Error: `Can't serialize value ... Expected 'java.time.OffsetDateTime' but was 'LocalDateTime'`
- The `graphql-java-extended-scalars` `DateTime` scalar requires `OffsetDateTime`, not `LocalDateTime`
- All JPA entities use `LocalDateTime` for database compatibility
- Fix: Add `@SchemaMapping` field resolvers that convert `LocalDateTime` → `OffsetDateTime` via `localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime()`
- These resolvers exist in `SyncJobQueryResolver` for `startTime`, `endTime`, `occurredAt`, `receivedAt`

### Port conflicts when starting dev services
- Error: `Port 3000 is in use, trying 3001 instead` — Next.js collides with Mock CRM API
- The improved `start-dev.sh` script auto-kills stale processes on ports 3000/3001/8080
- Manual fix: `lsof -ti :3000 | xargs kill` (repeat for 3001, 8080)

## Resources & References

- **Spring Boot Docs**: https://spring.io/projects/spring-boot
- **SQL Server Docker**: https://hub.docker.com/_/microsoft-mssql-server
- **Next.js Docs**: https://nextjs.org/docs
- **React Query**: https://tanstack.com/query/latest
- **LocalStack**: https://docs.localstack.cloud/
- **Flyway**: https://flywaydb.org/documentation/

## Notes

This project is designed to be portfolio-ready and demonstrates skills required for enterprise software engineering roles. It showcases real-world integration patterns, data quality management, and modern development practices.

For complete implementation guide with day-by-day tasks and Cursor AI prompts, see [docs/PROJECT_SPEC.md](./docs/PROJECT_SPEC.md).
