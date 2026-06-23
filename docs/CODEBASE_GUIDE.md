# Codebase Guide — Multi-Source Data Integration Platform

A technical deep-dive into the architecture, organization, and key patterns in this full-stack Spring Boot + React data integration system.

---

## Table of Contents

- [Backend Architecture](#backend-architecture)
- [Frontend Architecture](#frontend-architecture)
- [GraphQL Layer](#graphql-layer)
- [Database Schema](#database-schema)
- [Testing Strategy](#testing-strategy)
- [Infrastructure & DevOps](#infrastructure--devops)

---

## Backend Architecture

### High-Level Overview

**Tech stack**: Spring Boot 3.2, Java 17, Spring Data JPA, Spring Security, Spring Cloud AWS (SQS), Spring for GraphQL

**Core pattern**: ETL pipeline with four-schema database design + async processing via SQS queues

**Package structure**:
```
com.dataplatform/
├── config/           # Spring beans: Security, RestTemplate, AWS SQS, GraphQL scalars
├── controller/       # REST API endpoints (returns ResponseEntity<T>)
├── dto/              # Request/response objects for API boundaries
├── exception/        # Custom exceptions + global @RestControllerAdvice handler
├── graphql/          # GraphQL resolvers (queries, mutations, subscriptions)
├── integration/      # External API clients (CRM, ERP, Accounting) with retry logic
├── model/            # JPA entities mapped to database tables across all 4 schemas
├── repository/       # Spring Data JPA repositories (extends JpaRepository)
├── service/          # Business logic layer (orchestrates repositories + external calls)
├── sqs/              # SQS producer (send messages) + consumer (@SqsListener)
├── transformer/      # Data normalization (phone, email, address formatting)
└── validator/        # Business rule validation (required fields, formats, etc.)
```

**Key architectural principles**:
1. **Constructor injection** — all dependencies via `@RequiredArgsConstructor` (Lombok), no `@Autowired` fields
2. **Service layer is transactional** — use `@Transactional` on methods that modify data
3. **Per-record error isolation** — one bad record doesn't abort the whole sync job
4. **Async by default** — REST endpoints return 202 Accepted immediately, SQS handles background processing
5. **Stateless services** — no in-memory state, safe to horizontally scale

---

### Detailed Breakdown

#### 1. **ETL Pipeline Flow**

Every data source (CRM, ERP, Accounting) goes through the same five-stage pipeline:

```java
// 1. FETCH — CrmApiClient.java, ErpApiClient.java, AccountingApiClient.java
@Service
public class CrmApiClient {
    public PaginatedResponse<CrmCustomerResponse> fetchCustomers(int page, int size) {
        // RestTemplate with retry logic (3 attempts, exponential backoff)
        // Paginated fetch loops through all pages
    }
}

// 2. STAGE — CustomerIntegrationService.java (same pattern for products, invoices)
@Service
public class CustomerIntegrationService {
    public void syncCustomersForJob(SyncJob job) {
        // Fetch all pages from external API
        // Insert raw JSON into staging.raw_customers with sync_job_id
        // Per-record try/catch: errors logged to audit.sync_errors
    }
}

// 3. TRANSFORM — CustomerTransformationService.java
@Service
public class CustomerTransformationService {
    public TransformedCustomer transform(RawCustomer raw) {
        // Deserialize JSON via ObjectMapper
        // Normalize: phone (strip non-digits), email (lowercase), address (flatten)
    }
}

// 4. VALIDATE — CustomerValidationService.java
@Service
public class CustomerValidationService {
    public ValidationResult validate(TransformedCustomer customer) {
        // Stateless validation (no DB calls)
        // Collects ALL errors, not just first failure
        // Returns ValidationResult { valid, errors[], customer }
    }
}

// 5. LOAD — CustomerLoadService.java
@Service
public class CustomerLoadService {
    @Transactional
    public void load(TransformedCustomer customer, Long syncJobId) {
        // Upsert into validated.validated_customers (find-or-create by external_id)
        // Call stored procedure: EXEC [final].upsert_customers
        // Stored procedure uses MERGE for idempotent upsert
    }
}

// ORCHESTRATOR — CustomerPipelineService.java
@Service
public class CustomerPipelineService {
    public void runPipelineForJob(Long jobId) {
        // Fetch staging records for this sync job
        // For each record: transform → validate → load
        // Log errors back to audit.sync_errors (VALIDATION_ERROR, PIPELINE_ERROR)
        // Update sync job status to COMPLETED or FAILED
    }
}
```

**Key file to study**: `CustomerPipelineService.java` — shows how all five stages wire together with error handling.

---

#### 2. **Async Processing with SQS**

```java
// SqsConfig.java — Creates SQS queue + DLQ with redrive policy
@Configuration
@ConditionalOnProperty(name = "sqs.enabled", havingValue = "true", matchIfMissing = true)
public class SqsConfig {
    @Bean
    public SqsClient sqsClient() {
        // LocalStack endpoint in dev, real AWS in prod
        // Auto-creates "data-sync-queue" and "data-sync-dlq"
    }
}

// SyncMessageProducer.java — Sends sync requests to the queue
@Service
public class SyncMessageProducer {
    public void sendSyncRequest(Long jobId, String sourceName) {
        SyncMessage msg = new SyncMessage(jobId, sourceName, "FULL");
        String json = objectMapper.writeValueAsString(msg);
        sqsClient.sendMessage(req -> req.queueUrl(queueUrl).messageBody(json));
    }
}

// SyncMessageConsumer.java — Listens to the queue
@Service
@ConditionalOnProperty(name = "sqs.enabled", havingValue = "true", matchIfMissing = true)
public class SyncMessageConsumer {
    @SqsListener("data-sync-queue")
    public void processSyncMessage(String messageBody) {
        SyncMessage msg = objectMapper.readValue(messageBody, SyncMessage.class);
        syncJobService.startJob(msg.getJobId()); // QUEUED → RUNNING

        // Route to correct pipeline based on sourceName
        if ("CRM".equals(msg.getSourceName())) {
            customerPipelineService.runPipelineForJob(msg.getJobId());
        } else if ("ERP".equals(msg.getSourceName())) {
            productPipelineService.runPipelineForJob(msg.getJobId());
        } // ... etc

        syncJobService.completeJob(msg.getJobId()); // RUNNING → COMPLETED
    }
}

// IntegrationController.java — REST endpoint triggers async sync
@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {
    @PostMapping("/sync/customers")
    public ResponseEntity<SyncJobDTO> syncCustomers() {
        SyncJob job = syncJobService.createQueuedJob("CRM", SyncType.FULL);
        syncMessageProducer.sendSyncRequest(job.getId(), "CRM");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SyncJobDTO.fromEntity(job));
    }
}
```

**Flow**: POST `/api/integrations/sync/customers` → create QUEUED job → send SQS message → return 202 → consumer picks up message → run pipeline → update job to COMPLETED.

**Key file to study**: `SyncMessageConsumer.java` — shows multi-source routing and lifecycle transitions.

---

#### 3. **Database Schema Mapping**

JPA entities map to four schemas (staging, validated, final, audit):

```java
// audit.sync_jobs — tracks every sync operation
@Entity
@Table(name = "sync_jobs", schema = "audit")
public class SyncJob {
    @Id @GeneratedValue private Long id;
    private String sourceName; // "CRM", "ERP", "ACCOUNTING"
    @Enumerated(EnumType.STRING) private SyncStatus status; // QUEUED, RUNNING, COMPLETED, FAILED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer recordsProcessed;
    private Integer recordsFailed;

    @OneToMany(mappedBy = "syncJob") private List<SyncError> errors;
}

// staging.raw_customers — raw JSON payloads exactly as received
@Entity
@Table(name = "raw_customers", schema = "staging")
public class RawCustomer {
    @Id @GeneratedValue private Long id;
    @Column(columnDefinition = "TEXT") private String rawData; // JSON
    private String externalId;
    @ManyToOne @JoinColumn(name = "sync_job_id") private SyncJob syncJob;
    private LocalDateTime receivedAt;
}

// validated.validated_customers — cleaned, validated data
@Entity
@Table(name = "validated_customers", schema = "validated")
public class ValidatedCustomer {
    @Id @GeneratedValue private Long id;
    private String externalId;
    private String name;
    private String email;
    private String phone;
    private String address;
    @ManyToOne @JoinColumn(name = "sync_job_id") private SyncJob syncJob;
}

// [final].customers — production-ready data, upserted via stored procedure
@Entity
@Table(name = "customers", schema = "final")
public class FinalCustomer {
    @Id @GeneratedValue private Long id;
    @Column(unique = true) private String externalId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String sourceSystem; // "CRM"
    private LocalDateTime lastUpdated;
}
```

**Stored procedure** (`V2__customer_load_procedure.sql`):
```sql
CREATE PROCEDURE [final].upsert_customers
    @external_id NVARCHAR(255),
    @name NVARCHAR(255),
    @email NVARCHAR(255),
    @phone NVARCHAR(50),
    @address NVARCHAR(500),
    @source_system NVARCHAR(50)
AS
BEGIN
    MERGE [final].customers AS target
    USING (SELECT @external_id AS external_id) AS source
    ON target.external_id = source.external_id
    WHEN MATCHED THEN
        UPDATE SET name = @name, email = @email, phone = @phone,
                   address = @address, last_updated = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (external_id, name, email, phone, address, source_system, last_updated)
        VALUES (@external_id, @name, @email, @phone, @address, @source_system, GETDATE());
END
```

**Why MERGE?** Idempotent — safe to run the same sync multiple times without creating duplicates.

**Key files to study**:
- `SyncJob.java` — audit trail entity
- `RawCustomer.java` → `ValidatedCustomer.java` → `FinalCustomer.java` — three-stage data evolution
- `V2__customer_load_procedure.sql` — MERGE upsert pattern

---

#### 4. **Error Handling & Global Exception Handler**

```java
// Custom exceptions
public class IntegrationException extends RuntimeException {
    // Thrown when external API calls fail (maps to 502 Bad Gateway)
}

public class ResourceNotFoundException extends RuntimeException {
    // Thrown when a requested entity doesn't exist (maps to 404)
}

// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegration(IntegrationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(new ErrorResponse("INTEGRATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

**Per-record error handling** (in `CustomerIntegrationService.java`):
```java
for (CrmCustomerResponse customer : customers) {
    try {
        RawCustomer raw = new RawCustomer();
        raw.setRawData(objectMapper.writeValueAsString(customer));
        raw.setExternalId(customer.getId());
        raw.setSyncJob(job);
        rawCustomerRepository.save(raw);
    } catch (Exception e) {
        SyncError error = new SyncError();
        error.setSyncJob(job);
        error.setErrorType("STAGING_ERROR");
        error.setErrorMessage(e.getMessage());
        error.setFailedRecord(customer.toString());
        syncErrorRepository.save(error);
    }
}
```

**Key principle**: Isolate failures at the individual record level. One bad email format doesn't crash the entire sync of 100 customers.

---

#### 5. **Configuration Files**

```yaml
# application.yml — Main configuration
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=dataintegration;encrypt=true;trustServerCertificate=true
    username: sa
    password: ${DB_PASSWORD:YourStrong@Passw0rd}
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway manages schema, Hibernate just validates
    show-sql: false
  flyway:
    enabled: true
    schemas: audit,staging,validated,final
    baseline-on-migrate: true

# Integration client configs
integration:
  crm:
    base-url: http://localhost:3001
    page-size: 20
    max-retries: 3
  erp:
    base-url: http://localhost:3002
    page-size: 20
  accounting:
    base-url: http://localhost:3003
    page-size: 20

# SQS config
sqs:
  enabled: true
  endpoint: http://localhost:4566  # LocalStack
  queue-name: data-sync-queue
  dlq-name: data-sync-dlq
```

```yaml
# application-test.yml — Unit test profile (H2 in-memory)
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false

sqs:
  enabled: false  # Mock SqsClient bean in tests
```

```yaml
# application-integration-test.yml — Integration test profile (Testcontainers)
spring:
  datasource:
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
    # URL injected dynamically via @DynamicPropertySource from Testcontainers
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

sqs:
  enabled: false  # Integration tests don't need SQS

integration:
  crm:
    base-url: http://localhost:18089  # WireMock
  erp:
    base-url: http://localhost:18089
  accounting:
    base-url: http://localhost:18089
```

**Key files to study**: `application.yml` (base config), `application-test.yml` (H2 for unit tests), `application-integration-test.yml` (SQL Server for integration tests)

---

## Frontend Architecture

### High-Level Overview

**Tech stack**: React 18, TypeScript, Next.js 14 (App Router), TailwindCSS, Apollo Client (GraphQL), React Query (REST fallback)

**Core pattern**: Smart hooks + dumb components. Hooks encapsulate data fetching (GraphQL/REST), components focus on presentation.

**Directory structure**:
```
frontend/src/
├── app/                    # Next.js App Router (file-based routing)
│   ├── globals.css         # Tailwind directives (@tailwind base/components/utilities)
│   ├── layout.tsx          # Root layout (wraps all pages with Providers)
│   ├── page.tsx            # Dashboard (/)
│   ├── providers.tsx       # React Query + Apollo Client providers
│   └── jobs/[id]/page.tsx  # Job detail page (/jobs/123)
├── components/             # Reusable React components (PascalCase)
│   ├── ErrorAlert.tsx      # Red error banner
│   ├── LoadingSpinner.tsx  # Animated spinner (h-8 w-8 SVG)
│   ├── MetricCard.tsx      # Dashboard metric tile
│   ├── SourceSyncPanel.tsx # Three sync buttons (CRM/ERP/Accounting)
│   ├── StatusBadge.tsx     # Colored pill (QUEUED/RUNNING/COMPLETED/FAILED)
│   ├── SyncJobDetails.tsx  # Job summary grid + error log table
│   ├── SyncJobTable.tsx    # Sortable job list with row links
│   └── SyncMetricsChart.tsx # Recharts bar chart
├── graphql/                # Apollo Client queries/mutations/subscriptions
│   ├── queries/syncJobs.ts # GET_SYNC_JOB, GET_SYNC_JOBS, GET_DASHBOARD_DATA
│   ├── mutations/          # TRIGGER_SYNC, CANCEL_SYNC
│   └── subscriptions/      # WATCH_SYNC_JOB (WebSocket)
├── hooks/                  # Custom React hooks (prefix with 'use')
│   ├── useGraphQLDashboard.ts      # Apollo useQuery + 10s polling
│   ├── useGraphQLSyncJob.ts        # useQuery + useSubscription (real-time)
│   ├── useGraphQLTriggerSync.ts    # useMutation
│   ├── useSyncJobs.ts              # REST fallback (React Query)
│   └── useDashboardMetrics.ts      # Client-side metric computation
├── lib/
│   └── apollo-client.ts    # Apollo Client config (HTTP + WebSocket split link)
├── services/
│   └── syncJobService.ts   # REST API client (fetch wrappers)
└── types/
    └── syncJob.ts          # TypeScript interfaces (SyncJob, SyncError, etc.)
```

**Key architectural principles**:
1. **Server Components where possible** — Next.js 14 App Router defaults to server-side rendering
2. **Client Components when needed** — use `'use client'` directive for interactivity (useState, useEffect, Apollo hooks)
3. **Smart hooks, dumb components** — hooks handle data fetching/mutations, components receive props and render UI
4. **Polling only when necessary** — GraphQL queries poll at 10s intervals only when RUNNING/QUEUED jobs exist
5. **WebSocket subscriptions for real-time** — job detail page subscribes to status updates via GraphQL subscription

---

### Detailed Breakdown

#### 1. **Dashboard Page** (`app/page.tsx`)

```tsx
'use client'; // Needed for hooks (useState, Apollo useQuery)

export default function DashboardPage() {
  const { jobs, metrics, loading, error } = useGraphQLDashboard();
  const [sourceFilter, setSourceFilter] = useState<string | null>(null);

  // Filter jobs by source (CRM/ERP/Accounting)
  const filteredJobs = jobs.filter(j => !sourceFilter || j.sourceName === sourceFilter);

  return (
    <div className="min-h-screen bg-gray-50">
      <h1 className="text-2xl font-bold">Data Integration Dashboard</h1>
      <SourceSyncPanel /> {/* Three sync buttons */}

      {loading && !jobs.length ? (
        <LoadingSpinner />
      ) : (
        <>
          {/* Metric cards — 24h syncs, success rate, 30d records */}
          <MetricCard title="Syncs (24h)" value={metrics?.last24Hours.totalSyncs ?? 0} />

          {/* Bar chart — completed vs failed per day */}
          <SyncMetricsChart data={metrics?.last30Days.dailyStats ?? []} />

          {/* Source filter pills */}
          <button onClick={() => setSourceFilter('CRM')}>CRM</button>

          {/* Job table with status badges */}
          <SyncJobTable jobs={filteredJobs} />
        </>
      )}
    </div>
  );
}
```

**Key pattern**: The page is thin — it delegates to `useGraphQLDashboard()` for data and renders presentational components.

---

#### 2. **Smart Hooks** — Data Fetching & Mutations

**GraphQL Dashboard Hook** (`hooks/useGraphQLDashboard.ts`):
```ts
import { useQuery } from '@apollo/client';
import { GET_DASHBOARD_DATA } from '@/graphql/queries/syncJobs';

export function useGraphQLDashboard() {
  const { data, loading, error } = useQuery<DashboardData>(GET_DASHBOARD_DATA, {
    pollInterval: 10000, // Poll every 10 seconds
  });

  return {
    jobs: data?.syncJobs ?? [],
    metrics: data?.syncMetrics ?? null,
    loading,
    error,
  };
}
```

**GraphQL Mutation Hook** (`hooks/useGraphQLTriggerSync.ts`):
```ts
import { useMutation } from '@apollo/client';
import { TRIGGER_SYNC } from '@/graphql/mutations/triggerSync';

export function useGraphQLTriggerSync() {
  const [triggerSyncMutation, { loading, error }] = useMutation(TRIGGER_SYNC, {
    refetchQueries: ['GetDashboardData'], // Refresh dashboard after triggering
  });

  const triggerSync = (sourceName: string) => {
    return triggerSyncMutation({
      variables: { input: { sourceName, syncType: 'FULL' } },
    });
  };

  return { triggerSync, loading, error };
}
```

**GraphQL Subscription Hook** (`hooks/useGraphQLSyncJob.ts`):
```ts
import { useQuery, useSubscription } from '@apollo/client';
import { GET_SYNC_JOB } from '@/graphql/queries/syncJobs';
import { WATCH_SYNC_JOB } from '@/graphql/subscriptions/syncJobUpdated';

export function useGraphQLSyncJob(id: string) {
  // Initial query
  const { data, loading, error } = useQuery(GET_SYNC_JOB, {
    variables: { id },
    pollInterval: 5000, // Poll every 5s while RUNNING/QUEUED
  });

  // Real-time subscription (WebSocket)
  const { data: subData } = useSubscription(WATCH_SYNC_JOB, {
    variables: { id },
    skip: !['RUNNING', 'QUEUED'].includes(data?.syncJob?.status),
  });

  // Subscription data takes precedence over polling data
  const job = subData?.syncJobUpdated ?? data?.syncJob;

  return { job, loading, error };
}
```

**Key pattern**: Hooks abstract Apollo Client complexity — components call `useGraphQLDashboard()` and get `{ jobs, metrics, loading, error }`.

---

#### 3. **GraphQL Queries** (`graphql/queries/syncJobs.ts`)

```ts
import { gql } from '@apollo/client';

// Reusable fragment
export const SYNC_JOB_CORE_FIELDS = gql`
  fragment SyncJobCoreFields on SyncJob {
    id
    sourceName
    status
    startTime
    endTime
    recordsProcessed
    recordsFailed
    duration
    successRate
  }
`;

// Dashboard query — fetches jobs + metrics in one request
export const GET_DASHBOARD_DATA = gql`
  ${SYNC_JOB_CORE_FIELDS}
  query GetDashboardData {
    syncJobs(limit: 20, orderBy: START_TIME_DESC) {
      ...SyncJobCoreFields
      errors(limit: 3) {
        id
        errorMessage
        occurredAt
      }
    }
    syncMetrics(period: LAST_30_DAYS) {
      last24Hours {
        totalSyncs
        successRate
        totalRecords
      }
      last30Days {
        totalSyncs
        successRate
        totalRecords
        dailyStats {
          date
          syncsCompleted
          syncsFailed
        }
      }
    }
  }
`;
```

**Why fragments?** The same core fields are used in multiple queries (dashboard, job detail, subscriptions). Fragments ensure consistency and reduce duplication.

---

#### 4. **Apollo Client Configuration** (`lib/apollo-client.ts`)

```ts
import { ApolloClient, InMemoryCache, split, HttpLink } from '@apollo/client';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { getMainDefinition } from '@apollo/client/utilities';
import { createClient } from 'graphql-ws';

// HTTP for queries/mutations
const httpLink = new HttpLink({
  uri: 'http://localhost:8080/graphql',
  credentials: 'include', // Send cookies for auth
});

// WebSocket for subscriptions
const wsLink = new GraphQLWsLink(
  createClient({ url: 'ws://localhost:8080/graphql' })
);

// Split: use WebSocket for subscriptions, HTTP for everything else
const splitLink = split(
  ({ query }) => {
    const definition = getMainDefinition(query);
    return (
      definition.kind === 'OperationDefinition' &&
      definition.operation === 'subscription'
    );
  },
  wsLink,
  httpLink
);

export const apolloClient = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache({
    typePolicies: {
      Query: {
        fields: {
          syncJobs: {
            merge(_, incoming) {
              return incoming; // Replace cache, don't append
            },
          },
        },
      },
    },
  }),
  defaultOptions: {
    query: { fetchPolicy: 'network-only' }, // Always hit the server
  },
});
```

**Key pattern**: The `split` function routes GraphQL operations — subscriptions go over WebSocket, queries/mutations use HTTP. This is transparent to the components.

---

#### 5. **Dumb Components** — Presentation Only

**StatusBadge.tsx** (colored pill with pulse animation for RUNNING):
```tsx
export default function StatusBadge({ status }: { status: string }) {
  const colors = {
    QUEUED: 'bg-gray-100 text-gray-800',
    RUNNING: 'bg-blue-100 text-blue-800 animate-pulse',
    COMPLETED: 'bg-green-100 text-green-800',
    FAILED: 'bg-red-100 text-red-800',
  };

  return (
    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${colors[status]}`}>
      {status}
    </span>
  );
}
```

**SyncJobTable.tsx** (clickable rows linking to job detail):
```tsx
import { useRouter } from 'next/navigation';
import StatusBadge from './StatusBadge';

export default function SyncJobTable({ jobs }: { jobs: SyncJob[] }) {
  const router = useRouter();

  return (
    <table className="min-w-full divide-y divide-gray-200">
      <thead>
        <tr>
          <th>Source</th>
          <th>Status</th>
          <th>Records Processed</th>
          <th>Start Time</th>
        </tr>
      </thead>
      <tbody>
        {jobs.map((job) => (
          <tr
            key={job.id}
            onClick={() => router.push(`/jobs/${job.id}`)}
            className="cursor-pointer hover:bg-gray-50"
          >
            <td>{job.sourceName}</td>
            <td><StatusBadge status={job.status} /></td>
            <td>{job.recordsProcessed}</td>
            <td>{new Date(job.startTime).toLocaleString()}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

**Key pattern**: Components are purely presentational — they receive data via props, don't fetch data themselves, and focus on rendering UI with Tailwind classes.

---

#### 6. **TailwindCSS Styling**

```css
/* globals.css */
@tailwind base;
@tailwind components;
@tailwind utilities;
```

```js
// tailwind.config.js
module.exports = {
  content: ['./src/**/*.{ts,tsx}'], // Scan all TypeScript files for class names
  theme: {
    extend: {}, // Custom colors/spacing would go here
  },
  plugins: [],
};
```

**Utility-first approach**:
```tsx
<div className="min-h-screen bg-gray-50">           {/* Full viewport height, light gray bg */}
  <div className="mx-auto max-w-7xl px-4 py-8">     {/* Centered container, padding */}
    <h1 className="text-2xl font-bold text-gray-900"> {/* Large bold heading */}
      Dashboard
    </h1>
    <button className="rounded-md bg-blue-600 px-3 py-2 text-white hover:bg-blue-700">
      Sync                                           {/* Blue button with hover */}
    </button>
  </div>
</div>
```

**Why Tailwind?** No CSS files to manage, responsive design with `sm:` `md:` `lg:` prefixes, design consistency via utility classes.

---

## GraphQL Layer

### High-Level Overview

**Tech stack**: Spring for GraphQL, WebFlux (for subscriptions), Reactor (Sinks.Many for pub/sub), graphql-java-extended-scalars (DateTime/Date)

**Core pattern**: Resolver-based architecture. Resolvers are Spring beans annotated with `@QueryMapping`, `@MutationMapping`, `@SubscriptionMapping`, `@SchemaMapping` (field resolvers).

**Key files**:
```
backend/src/main/java/com/dataplatform/graphql/
├── SyncJobQueryResolver.java       # Queries: syncJob, syncJobs, syncMetrics
├── SyncJobMutationResolver.java    # Mutations: triggerSync, cancelSync
├── SyncJobSubscriptionResolver.java # Subscription: syncJobUpdated
└── SyncJobEventPublisher.java      # Reactor Sinks.Many for real-time events

backend/src/main/resources/graphql/
└── schema.graphqls                 # GraphQL schema definition (types, queries, mutations, subscriptions)
```

**Schema-first design**: The GraphQL schema (`schema.graphqls`) is the source of truth. Resolvers implement the schema.

---

### Detailed Breakdown

#### 1. **GraphQL Schema** (`schema.graphqls`)

```graphql
type Query {
  syncJob(id: ID!): SyncJob
  syncJobs(limit: Int = 20, offset: Int = 0, filter: SyncJobFilter, orderBy: SyncJobOrderBy): [SyncJob!]!
  syncMetrics(period: MetricsPeriod!): SyncMetrics
}

type Mutation {
  triggerSync(input: TriggerSyncInput!): SyncJob!
  cancelSync(jobId: ID!): SyncJob!
}

type Subscription {
  syncJobUpdated(id: ID!): SyncJob!
}

type SyncJob {
  id: ID!
  sourceName: String!
  status: SyncStatus!
  startTime: DateTime!
  endTime: DateTime
  recordsProcessed: Int!
  recordsFailed: Int!
  duration: Int           # Computed field (seconds)
  successRate: Float      # Computed field (percentage)
  errors(limit: Int = 10): [SyncError!]!
  validationStats: ValidationStats
}

enum SyncStatus {
  QUEUED
  RUNNING
  COMPLETED
  FAILED
}

scalar DateTime
scalar Date
```

**Computed fields** (`duration`, `successRate`) are calculated on the server via field resolvers, not stored in the database.

---

#### 2. **Query Resolver** (`SyncJobQueryResolver.java`)

```java
@Controller
public class SyncJobQueryResolver {
    private final SyncJobService syncJobService;

    @QueryMapping
    public SyncJob syncJob(@Argument String id) {
        return syncJobService.getJobById(Long.parseLong(id))
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    @QueryMapping
    public List<SyncJob> syncJobs(
        @Argument Integer limit,
        @Argument Integer offset,
        @Argument SyncJobFilter filter,
        @Argument SyncJobOrderBy orderBy
    ) {
        // Default limit 20, max 100
        int actualLimit = Math.min(limit != null ? limit : 20, 100);
        return syncJobService.getRecentJobs(actualLimit);
    }

    @QueryMapping
    public SyncMetrics syncMetrics(@Argument MetricsPeriod period) {
        List<SyncJob> jobs = syncJobService.getAllJobs();
        // Compute metrics from job list (success rate, avg duration, daily stats)
        return SyncMetrics.fromJobs(jobs, period);
    }
}
```

---

#### 3. **Field Resolvers** (Computed Fields + Nested Data)

```java
@Controller
public class SyncJobQueryResolver {

    // DateTime conversion: LocalDateTime (JPA) → OffsetDateTime (GraphQL scalar)
    @SchemaMapping(typeName = "SyncJob", field = "startTime")
    public OffsetDateTime startTime(SyncJob job) {
        return job.getStartTime().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    @SchemaMapping(typeName = "SyncJob", field = "endTime")
    public OffsetDateTime endTime(SyncJob job) {
        if (job.getEndTime() == null) return null;
        return job.getEndTime().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    // Computed field: duration in seconds
    @SchemaMapping(typeName = "SyncJob", field = "duration")
    public Integer duration(SyncJob job) {
        if (job.getEndTime() == null) return null;
        return (int) java.time.Duration.between(job.getStartTime(), job.getEndTime()).getSeconds();
    }

    // Computed field: success rate percentage
    @SchemaMapping(typeName = "SyncJob", field = "successRate")
    public Double successRate(SyncJob job) {
        int total = job.getRecordsProcessed() + job.getRecordsFailed();
        if (total == 0) return null;
        return (double) job.getRecordsProcessed() / total * 100.0;
    }

    // Nested data: fetch errors for this job
    @SchemaMapping(typeName = "SyncJob", field = "errors")
    public List<SyncError> errors(SyncJob job, @Argument Integer limit) {
        int actualLimit = limit != null ? limit : 10;
        return syncJobService.getErrorsForJob(job.getId()).stream()
            .limit(actualLimit)
            .toList();
    }
}
```

**Why field resolvers?**
- **Computed fields** — calculate `duration` and `successRate` on-the-fly without storing in the database
- **Type conversion** — JPA uses `LocalDateTime`, GraphQL `DateTime` scalar requires `OffsetDateTime`
- **N+1 prevention** — fetch nested `errors` only if the GraphQL query requests them (lazy loading)

---

#### 4. **Mutation Resolver** (`SyncJobMutationResolver.java`)

```java
@Controller
public class SyncJobMutationResolver {
    private final SyncJobService syncJobService;
    private final SyncMessageProducer syncMessageProducer;

    @MutationMapping
    public SyncJob triggerSync(@Argument TriggerSyncInput input) {
        // Create QUEUED job
        SyncJob job = syncJobService.createQueuedJob(input.getSourceName(), input.getSyncType());

        // Send SQS message (async processing)
        syncMessageProducer.sendSyncRequest(job.getId(), input.getSourceName());

        return job; // Returns immediately with QUEUED status
    }

    @MutationMapping
    public SyncJob cancelSync(@Argument String jobId) {
        SyncJob job = syncJobService.getJobById(Long.parseLong(jobId))
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getStatus() == SyncStatus.COMPLETED || job.getStatus() == SyncStatus.FAILED) {
            throw new IllegalArgumentException("Cannot cancel a completed or failed job");
        }

        syncJobService.failJob(job.getId(), "Cancelled by user");
        return syncJobService.getJobById(Long.parseLong(jobId)).orElseThrow();
    }
}
```

**Key pattern**: Mutations trigger side effects (create jobs, send SQS messages) and return the updated entity.

---

#### 5. **Subscription Resolver** (`SyncJobSubscriptionResolver.java`)

```java
@Controller
public class SyncJobSubscriptionResolver {
    private final SyncJobEventPublisher eventPublisher;

    @SubscriptionMapping
    public Flux<SyncJob> syncJobUpdated(@Argument String id) {
        return eventPublisher.subscribe()
            .filter(job -> job.getId().toString().equals(id)); // Only emit updates for this job
    }
}
```

**Event Publisher** (`SyncJobEventPublisher.java`):
```java
@Component
public class SyncJobEventPublisher {
    private final Sinks.Many<SyncJob> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(SyncJob job) {
        sink.tryEmitNext(job);
    }

    public Flux<SyncJob> subscribe() {
        return sink.asFlux();
    }
}
```

**Wired into service layer** (`SyncJobService.java`):
```java
@Service
public class SyncJobService {
    private final SyncJobEventPublisher eventPublisher;

    @Transactional
    public SyncJob completeJob(Long jobId) {
        SyncJob job = getJobEntity(jobId);
        job.setStatus(SyncStatus.COMPLETED);
        job.setEndTime(LocalDateTime.now());
        SyncJob saved = syncJobRepository.save(job);
        eventPublisher.publish(saved); // Publish to subscribers
        return saved;
    }
}
```

**Flow**: Service updates job → publishes event → Reactor `Sinks.Many` fans out → WebSocket sends to subscribed clients.

**Key files to study**:
- `SyncJobEventPublisher.java` — Reactor pub/sub pattern
- `SyncJobSubscriptionResolver.java` — filter events by job ID
- `SyncJobService.java` — where events get published (create/complete/fail methods)

---

## Database Schema

### High-Level Overview

**Four-schema architecture**:
1. **staging** — Raw JSON payloads exactly as received from external APIs
2. **validated** — Records that passed transformation + validation
3. **final** — Production-ready data, upserted via MERGE stored procedures
4. **audit** — Immutable log of all sync operations (sync_jobs, sync_errors)

**Migration tool**: Flyway — version-controlled SQL scripts in `backend/src/main/resources/db/migration/`

**Key principle**: Preserve raw data in staging so you can replay transformations if business logic changes.

---

### Detailed Breakdown

#### 1. **Flyway Migrations**

```
db/migration/
├── V1__initial_schema.sql           # Create 4 schemas + audit tables
├── V2__customer_tables_and_procedure.sql  # CRM tables + upsert_customers
├── V3__erp_tables_and_procedure.sql       # ERP tables + upsert_products
└── V4__accounting_tables_and_procedure.sql # Accounting tables + upsert_invoices
```

**V1 — Create schemas and audit tables**:
```sql
-- Create schemas
CREATE SCHEMA audit;
CREATE SCHEMA staging;
CREATE SCHEMA validated;
CREATE SCHEMA [final];

-- Audit tables
CREATE TABLE audit.sync_jobs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    source_name NVARCHAR(50) NOT NULL,
    sync_type NVARCHAR(20) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    start_time DATETIME2 NOT NULL,
    end_time DATETIME2 NULL,
    records_processed INT NOT NULL DEFAULT 0,
    records_failed INT NOT NULL DEFAULT 0,
    error_message NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE()
);

CREATE TABLE audit.sync_errors (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sync_job_id BIGINT NOT NULL,
    error_type NVARCHAR(50) NULL,
    error_message NVARCHAR(MAX) NOT NULL,
    failed_record NVARCHAR(MAX) NULL,
    occurred_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id)
);

CREATE INDEX idx_sync_jobs_status ON audit.sync_jobs(status);
CREATE INDEX idx_sync_jobs_start_time ON audit.sync_jobs(start_time DESC);
CREATE INDEX idx_sync_errors_job ON audit.sync_errors(sync_job_id);
```

---

#### 2. **Customer Pipeline Tables** (V2)

```sql
-- Staging: raw JSON exactly as received
CREATE TABLE staging.raw_customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id NVARCHAR(255) NULL,
    raw_data NVARCHAR(MAX) NOT NULL, -- JSON payload
    sync_job_id BIGINT NOT NULL,
    received_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id)
);

-- Validated: cleaned data that passed business rules
CREATE TABLE validated.validated_customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id NVARCHAR(255) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    email NVARCHAR(255) NULL,
    phone NVARCHAR(50) NULL,
    address NVARCHAR(500) NULL,
    sync_job_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id)
);

-- Final: production data with MERGE upsert
CREATE TABLE [final].customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id NVARCHAR(255) UNIQUE NOT NULL,
    name NVARCHAR(255) NOT NULL,
    email NVARCHAR(255) NULL,
    phone NVARCHAR(50) NULL,
    address NVARCHAR(500) NULL,
    source_system NVARCHAR(50) NOT NULL,
    last_updated DATETIME2 NOT NULL DEFAULT GETDATE()
);

CREATE INDEX idx_final_customers_external_id ON [final].customers(external_id);
```

---

#### 3. **Stored Procedure — Idempotent Upsert** (V2)

```sql
CREATE PROCEDURE [final].upsert_customers
    @external_id NVARCHAR(255),
    @name NVARCHAR(255),
    @email NVARCHAR(255),
    @phone NVARCHAR(50),
    @address NVARCHAR(500),
    @source_system NVARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;

    MERGE [final].customers AS target
    USING (SELECT @external_id AS external_id) AS source
    ON target.external_id = source.external_id
    WHEN MATCHED THEN
        UPDATE SET
            name = @name,
            email = @email,
            phone = @phone,
            address = @address,
            last_updated = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (external_id, name, email, phone, address, source_system, last_updated)
        VALUES (@external_id, @name, @email, @phone, @address, @source_system, GETDATE());
END
```

**Why MERGE?** Combines INSERT and UPDATE in a single atomic operation. Idempotent — running it twice with the same data doesn't create duplicates.

**Invoked from Java** (`CustomerLoadService.java`):
```java
@Transactional
public void load(TransformedCustomer customer, Long syncJobId) {
    // Save to validated schema
    ValidatedCustomer validated = new ValidatedCustomer();
    validated.setExternalId(customer.getExternalId());
    validated.setName(customer.getName());
    validated.setSyncJob(syncJobService.getJobEntity(syncJobId));
    validatedCustomerRepository.save(validated);

    // Call stored procedure for final schema
    String sql = "EXEC [final].upsert_customers :externalId, :name, :email, :phone, :address, :sourceSystem";
    entityManager.createNativeQuery(sql)
        .setParameter("externalId", customer.getExternalId())
        .setParameter("name", customer.getName())
        .setParameter("email", customer.getEmail())
        .setParameter("phone", customer.getPhone())
        .setParameter("address", customer.getAddress())
        .setParameter("sourceSystem", "CRM")
        .executeUpdate();
}
```

---

#### 4. **Data Flow Through Schemas**

```
1. External API → CrmApiClient.fetchCustomers()
   ↓
2. staging.raw_customers (raw JSON inserted)
   ↓
3. CustomerTransformationService.transform() → normalize phone/email/address
   ↓
4. CustomerValidationService.validate() → check required fields, email format
   ↓
5. validated.validated_customers (clean data inserted)
   ↓
6. EXEC [final].upsert_customers (MERGE: update if exists, insert if new)
   ↓
7. [final].customers (production data)
```

**Audit trail**: Every step logs to `audit.sync_errors` if a record fails validation or transformation.

---

## Testing Strategy

### High-Level Overview

**Two test suites with different tooling**:

1. **Unit tests** — Fast, isolated, H2 in-memory database, mocked dependencies (127 tests, ~15 seconds)
2. **Integration tests** — Slow, realistic, Testcontainers SQL Server, WireMock HTTP stubs (20 tests, ~90 seconds)

**Test execution**:
- `./mvnw test` — unit tests only (no Docker required)
- `./mvnw verify` — unit + integration tests (requires Docker)
- `./mvnw failsafe:integration-test failsafe:verify` — integration tests only

**Maven plugin split**:
- `maven-surefire-plugin` — runs `*Test.java` files, excludes `*IntegrationTest.java`
- `maven-failsafe-plugin` — runs `*IntegrationTest.java` files only

---

### Detailed Breakdown

#### 1. **Unit Test Example** (`CustomerPipelineServiceTest.java`)

```java
@ExtendWith(MockitoExtension.class)
public class CustomerPipelineServiceTest {

    @Mock private RawCustomerRepository rawCustomerRepository;
    @Mock private CustomerTransformationService transformationService;
    @Mock private CustomerValidationService validationService;
    @Mock private CustomerLoadService loadService;
    @Mock private SyncJobService syncJobService;
    @Mock private SyncErrorRepository syncErrorRepository;

    @InjectMocks private CustomerPipelineService pipelineService;

    @Test
    void runPipeline_allRecordsSucceed() {
        // Given: 2 raw customers in staging
        SyncJob job = new SyncJob();
        job.setId(1L);
        when(syncJobService.getJobEntity(1L)).thenReturn(job);

        RawCustomer raw1 = new RawCustomer();
        raw1.setRawData("{\"id\":\"c1\",\"name\":\"Alice\"}");
        RawCustomer raw2 = new RawCustomer();
        raw2.setRawData("{\"id\":\"c2\",\"name\":\"Bob\"}");
        when(rawCustomerRepository.findBySyncJob(job)).thenReturn(List.of(raw1, raw2));

        // When: transform → validate → load (all succeed)
        TransformedCustomer t1 = new TransformedCustomer("c1", "Alice", null, null, null, raw1.getRawData());
        when(transformationService.transform(raw1)).thenReturn(t1);
        when(validationService.validate(t1)).thenReturn(new ValidationResult(true, t1, List.of()));

        TransformedCustomer t2 = new TransformedCustomer("c2", "Bob", null, null, null, raw2.getRawData());
        when(transformationService.transform(raw2)).thenReturn(t2);
        when(validationService.validate(t2)).thenReturn(new ValidationResult(true, t2, List.of()));

        // Then: pipeline runs successfully, no errors logged
        pipelineService.runPipelineForJob(1L);

        verify(loadService, times(2)).load(any(), eq(1L));
        verify(syncErrorRepository, never()).save(any());
    }

    @Test
    void runPipeline_validationFails_logsErrorAndContinues() {
        // Given: 1 raw customer that fails validation
        SyncJob job = new SyncJob();
        job.setId(1L);
        when(syncJobService.getJobEntity(1L)).thenReturn(job);

        RawCustomer raw = new RawCustomer();
        raw.setRawData("{\"id\":\"c1\"}"); // Missing required 'name' field
        when(rawCustomerRepository.findBySyncJob(job)).thenReturn(List.of(raw));

        TransformedCustomer transformed = new TransformedCustomer("c1", null, null, null, null, raw.getRawData());
        when(transformationService.transform(raw)).thenReturn(transformed);
        when(validationService.validate(transformed))
            .thenReturn(new ValidationResult(false, transformed, List.of("Name is required")));

        // When: pipeline runs
        pipelineService.runPipelineForJob(1L);

        // Then: error logged to audit.sync_errors, load service NOT called
        verify(syncErrorRepository).save(argThat(error ->
            error.getErrorType().equals("VALIDATION_ERROR") &&
            error.getErrorMessage().contains("Name is required")
        ));
        verify(loadService, never()).load(any(), any());
    }
}
```

**Key patterns**:
- Use `@Mock` for all dependencies, `@InjectMocks` for the service under test
- Test happy path (all succeed) AND error paths (validation fails, transform throws exception)
- Verify interactions: `verify(loadService, times(2)).load(...)` ensures the service was called correctly

---

#### 2. **Integration Test Base Class** (`BaseIntegrationTest.java`)

```java
@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
public abstract class BaseIntegrationTest {

    // Shared Azure SQL Edge container (ARM-native for Apple Silicon)
    @Container
    static MSSQLServerContainer<?> sqlServer = new MSSQLServerContainer<>(
        DockerImageName.parse("mcr.microsoft.com/azure-sql-edge:latest")
            .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")
    )
    .acceptLicense()
    .withInitScript("integration-test-init.sql"); // Creates 'dataintegration' database

    // Static WireMock server for HTTP API stubbing
    static final int WIREMOCK_PORT = 18089;
    static WireMockServer wireMock;

    @BeforeAll
    static void setupWireMock() {
        wireMock = new WireMockServer(wireMockConfig().port(WIREMOCK_PORT));
        wireMock.start();
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Inject Testcontainers' dynamic JDBC URL into Spring context
        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
    }

    @Autowired protected SyncJobRepository syncJobRepository;
    @Autowired protected SyncErrorRepository syncErrorRepository;
    @Autowired protected RawCustomerRepository rawCustomerRepository;
    @Autowired protected ValidatedCustomerRepository validatedCustomerRepository;
    @Autowired protected FinalCustomerRepository finalCustomerRepository;

    @BeforeEach
    void cleanupDatabase() {
        // Truncate all schemas between tests for isolation
        syncErrorRepository.deleteAll();
        syncJobRepository.deleteAll();
        rawCustomerRepository.deleteAll();
        validatedCustomerRepository.deleteAll();
        finalCustomerRepository.deleteAll();
    }
}
```

**Key patterns**:
- `@Container static` — Testcontainer is shared across all test classes (starts once, reused)
- `@DynamicPropertySource` — inject the container's dynamic JDBC URL into Spring's context
- `@BeforeEach cleanupDatabase()` — truncate all tables between tests for isolation

---

#### 3. **Integration Test Example** (`CustomerPipelineIntegrationTest.java`)

```java
public class CustomerPipelineIntegrationTest extends BaseIntegrationTest {

    @Autowired private CustomerPipelineService pipelineService;
    @Autowired private SyncJobService syncJobService;

    @Test
    void fullPipeline_fetchTransformValidateLoad_allSchemas() {
        // Given: WireMock returns 2 customers
        WireMockStubs.stubCrmCustomers(wireMock, List.of(
            new CrmCustomerResponse("c1", "Alice", "alice@example.com", "1234567890",
                new Address("123 Main St", "Springfield", "IL", "62701")),
            new CrmCustomerResponse("c2", "Bob", "bob@example.com", "9876543210",
                new Address("456 Oak Ave", "Chicago", "IL", "60601"))
        ));

        // When: Trigger sync and run full pipeline
        SyncJob job = syncJobService.createQueuedJob("CRM", SyncType.FULL);
        customerIntegrationService.syncCustomersForJob(job); // Stage
        pipelineService.runPipelineForJob(job.getId());       // Transform, Validate, Load

        // Then: Verify data in all 4 schemas

        // 1. Audit: job completed with 2 records processed
        SyncJob completed = syncJobRepository.findById(job.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(SyncStatus.COMPLETED);
        assertThat(completed.getRecordsProcessed()).isEqualTo(2);
        assertThat(completed.getRecordsFailed()).isEqualTo(0);

        // 2. Staging: raw JSON preserved
        List<RawCustomer> staged = rawCustomerRepository.findBySyncJob(job);
        assertThat(staged).hasSize(2);
        assertThat(staged.get(0).getRawData()).contains("\"name\":\"Alice\"");

        // 3. Validated: cleaned data
        List<ValidatedCustomer> validated = validatedCustomerRepository.findAll();
        assertThat(validated).hasSize(2);
        assertThat(validated.get(0).getName()).isEqualTo("Alice");
        assertThat(validated.get(0).getPhone()).isEqualTo("1234567890"); // Digits only

        // 4. Final: production data
        List<FinalCustomer> finalCustomers = finalCustomerRepository.findAll();
        assertThat(finalCustomers).hasSize(2);
        assertThat(finalCustomers.get(0).getSourceSystem()).isEqualTo("CRM");
    }

    @Test
    void mergeUpsert_updateExistingRecord() {
        // Given: Customer already exists in final schema
        FinalCustomer existing = new FinalCustomer();
        existing.setExternalId("c1");
        existing.setName("Alice Old");
        existing.setEmail("old@example.com");
        existing.setSourceSystem("CRM");
        finalCustomerRepository.save(existing);

        // When: Re-sync with updated data
        WireMockStubs.stubCrmCustomers(wireMock, List.of(
            new CrmCustomerResponse("c1", "Alice Updated", "new@example.com", "1111111111", null)
        ));

        SyncJob job = syncJobService.createQueuedJob("CRM", SyncType.FULL);
        customerIntegrationService.syncCustomersForJob(job);
        pipelineService.runPipelineForJob(job.getId());

        // Then: Final table still has 1 record (MERGE updated, didn't insert duplicate)
        List<FinalCustomer> finalCustomers = finalCustomerRepository.findAll();
        assertThat(finalCustomers).hasSize(1);
        assertThat(finalCustomers.get(0).getName()).isEqualTo("Alice Updated");
        assertThat(finalCustomers.get(0).getEmail()).isEqualTo("new@example.com");
    }
}
```

**Key patterns**:
- **WireMock stubs** — control what the external API returns
- **Verify data in all 4 schemas** — staging, validated, final, audit
- **Test MERGE idempotency** — re-sync the same external_id updates the record, doesn't duplicate

---

#### 4. **WireMock Stubs** (`WireMockStubs.java`)

```java
public class WireMockStubs {

    public static void stubCrmCustomers(WireMockServer wireMock, List<CrmCustomerResponse> customers) {
        PaginatedResponse<CrmCustomerResponse> response = new PaginatedResponse<>();
        response.setContent(customers);
        response.setTotalElements(customers.size());
        response.setTotalPages(1);

        wireMock.stubFor(get(urlPathEqualTo("/api/customers"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(new ObjectMapper().writeValueAsString(response))));
    }

    public static void stubCrmFailure(WireMockServer wireMock) {
        wireMock.stubFor(get(urlPathEqualTo("/api/customers"))
            .willReturn(aResponse().withStatus(500)));
    }
}
```

**Key pattern**: Factory methods for common stubbing scenarios — success with data, failure with 500, pagination.

---

## Infrastructure & DevOps

### High-Level Overview

**Local development**:
- `docker-compose.yml` — SQL Server + LocalStack (SQS)
- `start-dev.sh` — one-command startup for all 5 services

**CI/CD**:
- `.github/workflows/ci.yml` — GitHub Actions workflow
- Runs on every push to `main` and every PR

**Database migrations**:
- Flyway auto-runs on Spring Boot startup (`spring.flyway.enabled=true`)

---

### Detailed Breakdown

#### 1. **Docker Compose** (`docker-compose.yml`)

```yaml
version: '3.8'

services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2019-latest
    container_name: data-integration-sqlserver
    ports:
      - "1433:1433"
    environment:
      ACCEPT_EULA: "Y"
      SA_PASSWORD: "YourStrong@Passw0rd"
      MSSQL_PID: "Developer"
    volumes:
      - sqlserver_data:/var/opt/mssql
      - ./database/init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourStrong@Passw0rd" -Q "SELECT 1" -C
      interval: 10s
      timeout: 5s
      retries: 5

  localstack:
    image: localstack/localstack:latest
    container_name: data-integration-localstack
    ports:
      - "4566:4566"
    environment:
      SERVICES: sqs,s3
      DEBUG: 1
      DATA_DIR: /tmp/localstack/data
    volumes:
      - localstack_data:/tmp/localstack
      - ./localstack/init-scripts:/etc/localstack/init/ready.d

volumes:
  sqlserver_data:
  localstack_data:
```

**LocalStack init script** (`localstack/init-scripts/01-create-queues.sh`):
```bash
#!/bin/bash
awslocal sqs create-queue --queue-name data-sync-queue
awslocal sqs create-queue --queue-name data-sync-dlq

# Set redrive policy on main queue
DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url http://localhost:4566/000000000000/data-sync-dlq --attribute-names QueueArn | jq -r '.Attributes.QueueArn')
awslocal sqs set-queue-attributes --queue-url http://localhost:4566/000000000000/data-sync-queue \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"
```

---

#### 2. **Start Dev Script** (`start-dev.sh`)

```bash
#!/usr/bin/env bash

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS=()

cleanup() {
  echo "Shutting down services..."
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null
  echo "All services stopped."
}
trap cleanup EXIT INT TERM

# Kill stale processes
for port in 3000 3001 3002 3003 8080; do
  pid=$(lsof -ti :"$port" 2>/dev/null)
  [ -n "$pid" ] && kill "$pid" 2>/dev/null || true
done

# Start all 5 services
cd "$ROOT_DIR/mock-apis/crm-api" && npm start & PIDS+=($!)
cd "$ROOT_DIR/mock-apis/erp-api" && npm start & PIDS+=($!)
cd "$ROOT_DIR/mock-apis/accounting-api" && npm start & PIDS+=($!)
cd "$ROOT_DIR/backend" && ./mvnw spring-boot:run & PIDS+=($!)
cd "$ROOT_DIR/frontend" && npm run dev & PIDS+=($!)

echo "Press Ctrl+C to stop all services."
wait
```

---

#### 3. **GitHub Actions CI** (`.github/workflows/ci.yml`)

```yaml
name: CI Pipeline

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  backend-unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
      - name: Run unit tests
        run: cd backend && ./mvnw test

  backend-integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
      - name: Run integration tests
        run: cd backend && ./mvnw failsafe:integration-test failsafe:verify

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - name: Cache npm dependencies
        uses: actions/cache@v3
        with:
          path: ~/.npm
          key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
      - name: Install dependencies
        run: cd frontend && npm ci
      - name: Lint
        run: cd frontend && npm run lint
      - name: Type check
        run: cd frontend && npx tsc --noEmit
      - name: Build
        run: cd frontend && npm run build
```

**Three parallel jobs**:
1. **backend-unit-tests** — `./mvnw test` (127 tests, H2, ~15s)
2. **backend-integration-tests** — `./mvnw verify` (20 tests, Testcontainers, ~90s)
3. **frontend** — lint → type-check → build

---

## Recommended Study Path

### Beginner Track (Start Here)

1. **Start with the demo** — Run `./start-dev.sh`, trigger a sync, watch the dashboard update. This gives you the "why" before diving into the "how".

2. **Read the schemas** — Open `V1__initial_schema.sql` and understand the four-schema design. This is the foundation.

3. **Trace one customer** — Pick a single CRM customer sync and follow the code path:
   - `IntegrationController.syncCustomers()` → creates QUEUED job
   - `SyncMessageConsumer.processSyncMessage()` → picks up from SQS
   - `CustomerPipelineService.runPipelineForJob()` → orchestrates transform/validate/load
   - Check the database: staging → validated → final

4. **Study one component** — Pick `StatusBadge.tsx` or `SyncJobTable.tsx`. Understand how Tailwind classes style the UI and how props flow from parent to child.

### Advanced Track (Deep Dive)

1. **GraphQL subscriptions** — Read `SyncJobEventPublisher.java` (Reactor pub/sub) → `SyncJobSubscriptionResolver.java` → frontend `useGraphQLSyncJob.ts` (WebSocket). Understand how real-time updates flow server → client.

2. **Integration tests** — Read `BaseIntegrationTest.java` and `CustomerPipelineIntegrationTest.java`. Understand how Testcontainers spins up a real SQL Server and how `@DynamicPropertySource` injects the JDBC URL.

3. **Error isolation** — Read `CustomerIntegrationService.syncCustomersForJob()` and trace how a single bad record (invalid JSON, missing field) gets logged to `audit.sync_errors` without aborting the entire batch.

4. **MERGE stored procedure** — Read `V2__customer_load_procedure.sql` and understand why MERGE is idempotent. Then read `CustomerLoadService.load()` to see how Spring calls the stored procedure via native SQL.

5. **Apollo Client cache** — Read `lib/apollo-client.ts`, specifically the `typePolicies` configuration. Understand why `syncJobs` has a `merge()` function that replaces instead of appending.

---

**Questions? Pick a file from the sections above and read it alongside this guide. The code is the ultimate source of truth.**
