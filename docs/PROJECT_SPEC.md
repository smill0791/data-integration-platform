# Multi-Source Data Integration Platform
## Technical Specification & Implementation Guide

**February 2026**

A comprehensive enterprise data integration system built with Spring Boot, SQL Server, React, and AWS

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Technical Architecture](#technical-architecture)
3. [Technology Stack Details](#technology-stack-details)
4. [Project Structure](#project-structure)
5. [Implementation Phases](#implementation-phases)
6. [Database Schema Design](#database-schema-design)
7. [Key Features & Capabilities](#key-features--capabilities)
8. [Testing Strategy](#testing-strategy)
9. [Development Setup Guide](#development-setup-guide)
10. [Cursor Development Tips](#cursor-development-tips)
11. [Next Steps & Extensions](#next-steps--extensions)
12. [Conclusion](#conclusion)

---

## Executive Summary

### Project Overview

The Multi-Source Data Integration Platform is an enterprise-grade system designed to demonstrate proficiency in modern full-stack development technologies including Spring Boot, SQL Server, React, and AWS. This project simulates real-world data integration challenges faced by financial services and enterprise organizations.

### Business Problem

Enterprise organizations need to pull data from various systems (CRM, ERP, databases, APIs), transform it according to business rules, validate data quality, and load it into a central warehouse for reporting and analytics. This process must be:

- **Reliable** - handle failures gracefully with retry mechanisms
- **Auditable** - track all data movements and transformations
- **Scalable** - process large volumes of data efficiently
- **Maintainable** - clear separation of concerns and testable code
- **Observable** - provide visibility into sync status and data quality

### Key Technologies Demonstrated

- **Backend**: Spring Boot 3.x, Spring Data JPA, Spring Security, Java 17+
- **Database**: SQL Server with complex schemas, stored procedures, performance optimization
- **Frontend**: React 18, TypeScript, Next.js, TailwindCSS
- **Cloud**: AWS (SQS for message queuing, S3 for file storage)
- **Testing**: JUnit 5, Mockito, Jest, Playwright
- **Infrastructure**: Docker, Docker Compose, LocalStack

---

## Technical Architecture

### System Components

The platform consists of six primary layers:

#### 1. Frontend Layer

- **Technology**: React, TypeScript, Next.js, TailwindCSS
- **Purpose**: Dashboard for monitoring sync jobs, triggering integrations, viewing data quality metrics
- **Features**: Real-time status updates, error visualization, configuration management

#### 2. API Gateway Layer

- **Technology**: Spring Boot REST Controllers, Spring Security
- **Purpose**: Expose RESTful APIs for frontend and external systems
- **Security**: JWT authentication, role-based access control, CORS configuration

#### 3. Business Logic Layer

Three primary services orchestrate the integration workflow:

- **Integration Service**: Fetches data from external sources, manages pagination, handles API errors
- **Scheduler Service**: Triggers scheduled syncs, manages cron jobs, monitors system health
- **Reporting Service**: Generates data quality reports, exports analytics, serves aggregated metrics

#### 4. Message Queue Layer

- **Technology**: AWS SQS (LocalStack for development)
- **Purpose**: Decouple sync triggering from execution, enable async processing
- **Benefits**: Scalability, retry logic, dead-letter queues for failed jobs

#### 5. Data Layer

SQL Server database organized into four schemas:

- **Staging Schema**: Raw data from external sources (unmodified JSON payloads)
- **Validated Schema**: Data that passed validation rules (clean, structured)
- **Final Schema**: Production-ready data available for reporting
- **Audit Schema**: Sync job tracking, error logs, data lineage

#### 6. External Data Sources

Mock external systems simulating real-world integrations:

- CRM API - customer data (Express.js mock service)
- ERP API - order and inventory data
- Accounting API - financial transactions
- File Sources - CSV/JSON uploads to S3

### Architecture Diagram (Text)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend Layer                            │
│  React + TypeScript + TailwindCSS (Dashboard & Configuration)    │
└────────────────────┬────────────────────────────────────────────┘
                     │ REST API
┌────────────────────▼────────────────────────────────────────────┐
│                     API Gateway Layer                            │
│         Spring Boot REST Controllers + Spring Security           │
│              (Authentication, Authorization, CORS)               │
└────────────────────┬────────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼──────┐ ┌──▼────────┐ ┌▼──────────────┐
│ Integration  │ │ Scheduler │ │   Reporting   │
│   Service    │ │  Service  │ │    Service    │
│              │ │           │ │               │
│ - Fetch data │ │ - Cron    │ │ - Query data  │
│ - Transform  │ │   jobs    │ │ - Generate    │
│ - Validate   │ │ - Trigger │ │   reports     │
│ - Load       │ │   syncs   │ │ - Export      │
└──────┬───────┘ └───────────┘ └───────────────┘
       │
       │ Async Processing
┌──────▼──────────────────────────────────────────────────────────┐
│                    Message Queue Layer                           │
│                  AWS SQS (or LocalStack for dev)                 │
│              - Data import jobs                                  │
│              - Retry failed operations                           │
│              - Decoupling & scalability                          │
└──────┬──────────────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────────────┐
│                      Data Layer                                  │
│                    SQL Server Database                           │
│                                                                  │
│  Staging Schema:        Validation Schema:    Final Schema:     │
│  - raw_customers        - validated_customers - customers       │
│  - raw_orders           - validated_orders    - orders         │
│  - raw_products         - validated_products  - products       │
│                                                                  │
│  Audit Schema:                                                  │
│  - sync_jobs (status, timestamps, record counts)                │
│  - sync_errors (error details, failed records)                  │
│  - data_lineage (track transformations)                         │
└──────┬──────────────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────────────┐
│                    External Data Sources                         │
│                                                                  │
│  Mock REST APIs (you'll build these):                           │
│  - "CRM API" - customer data                                    │
│  - "ERP API" - order/inventory data                             │
│  - "Accounting API" - financial transactions                    │
│                                                                  │
│  File Sources (S3 or local):                                    │
│  - CSV uploads                                                   │
│  - JSON batch files                                             │
└──────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack Details

### Backend Technologies

- **Java 17 or 21**: Modern Java features including records, sealed classes, pattern matching
- **Spring Boot 3.x**: Framework for building production-ready applications
- **Spring Data JPA**: Database access with Hibernate ORM
- **Spring Security**: Authentication, authorization, CORS, CSRF protection
- **AWS SDK for Java**: Integration with SQS and S3
- **Lombok**: Reduce boilerplate code (getters, setters, builders)
- **JUnit 5 + Mockito**: Unit and integration testing

### Database Technologies

- **SQL Server 2019+**: Enterprise relational database
- **Flyway**: Database migration and version control

### Frontend Technologies

- **React 18**: Modern UI library with hooks
- **TypeScript**: Type-safe JavaScript development
- **Next.js**: React framework with routing and API routes
- **TailwindCSS**: Utility-first CSS framework
- **React Query**: Data fetching and state management
- **Recharts**: Data visualization library

### Infrastructure Technologies

- **Docker**: Containerization for consistent environments
- **Docker Compose**: Multi-container orchestration for local development
- **LocalStack**: Mock AWS services for local development
- **GitHub Actions**: CI/CD pipeline automation

---

## Project Structure

```
data-integration-platform/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/dataplatform/
│   │   │   │   ├── config/           # Spring configuration
│   │   │   │   ├── controller/       # REST controllers
│   │   │   │   ├── service/          # Business logic
│   │   │   │   ├── repository/       # Data access
│   │   │   │   ├── model/            # Domain entities
│   │   │   │   ├── dto/              # Data transfer objects
│   │   │   │   ├── integration/      # External API clients
│   │   │   │   ├── transformer/      # Data transformation
│   │   │   │   ├── validator/        # Data validation
│   │   │   │   └── exception/        # Custom exceptions
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/     # Flyway scripts
│   │   └── test/
│   │       └── java/com/dataplatform/
│   ├── pom.xml (or build.gradle)
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── app/                      # Next.js app directory
│   │   ├── components/               # React components
│   │   ├── hooks/                    # Custom hooks
│   │   ├── services/                 # API clients
│   │   └── types/                    # TypeScript types
│   ├── package.json
│   └── Dockerfile
├── mock-apis/                        # Simulated external systems
│   └── crm-api/
├── database/
│   └── init-scripts/                 # SQL Server setup
├── docker-compose.yml
├── .cursorrules                      # Cursor AI configuration
└── README.md
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1)

#### Day 1: Initialize Spring Boot Backend

**Tasks:**
- Create Spring Boot project with required dependencies
- Configure application.yml for SQL Server connection
- Create initial domain models (SyncJob, SyncError, Customer)
- Set up basic project structure with packages

**Cursor Prompt:**
```
Create a Spring Boot 3.2 project with the following dependencies:
- Spring Web
- Spring Data JPA
- Spring Security
- SQL Server Driver
- Lombok
- Validation
- Spring Boot DevTools

Use Java 17, Maven, and package name com.dataplatform
```

#### Day 1-2: Set Up Infrastructure

**Tasks:**
- Create docker-compose.yml for SQL Server and LocalStack
- Start containers and verify connectivity
- Connect to SQL Server using Azure Data Studio or similar tool

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2019-latest
    container_name: integration-sqlserver
    environment:
      - ACCEPT_EULA=Y
      - SA_PASSWORD=YourStrong@Passw0rd
      - MSSQL_PID=Express
    ports:
      - "1433:1433"
    volumes:
      - sqlserver-data:/var/opt/mssql

  localstack:
    image: localstack/localstack:latest
    container_name: integration-localstack
    ports:
      - "4566:4566"
    environment:
      - SERVICES=sqs,s3
      - DEBUG=1
    volumes:
      - localstack-data:/var/lib/localstack

volumes:
  sqlserver-data:
  localstack-data:
```

#### Day 2: Create Database Schema

**Tasks:**
- Create Flyway migration scripts for all four schemas
- Define tables for staging, validated, final, and audit data
- Add indexes for performance optimization
- Verify schema creation by running Spring Boot application

**Migration File:** `src/main/resources/db/migration/V1__initial_schema.sql`

```sql
-- Audit schema
CREATE SCHEMA audit;

-- Staging schema (raw data from sources)
CREATE SCHEMA staging;

-- Validated schema (after validation rules)
CREATE SCHEMA validated;

-- Final schema (production data)
CREATE SCHEMA final;

-- Sync job tracking
CREATE TABLE audit.sync_jobs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    source_name VARCHAR(100) NOT NULL,
    sync_type VARCHAR(50) NOT NULL, -- 'FULL', 'INCREMENTAL'
    status VARCHAR(50) NOT NULL, -- 'RUNNING', 'COMPLETED', 'FAILED'
    start_time DATETIME2 NOT NULL,
    end_time DATETIME2,
    records_processed INT DEFAULT 0,
    records_failed INT DEFAULT 0,
    error_message VARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    INDEX idx_status (status),
    INDEX idx_source_start (source_name, start_time)
);

-- Error logging
CREATE TABLE audit.sync_errors (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sync_job_id BIGINT NOT NULL,
    error_type VARCHAR(100),
    error_message VARCHAR(MAX),
    failed_record VARCHAR(MAX), -- JSON of the record that failed
    occurred_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id),
    INDEX idx_sync_job (sync_job_id)
);

-- Staging: Raw customer data
CREATE TABLE staging.raw_customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sync_job_id BIGINT NOT NULL,
    external_id VARCHAR(100),
    raw_data VARCHAR(MAX), -- Store full JSON payload
    received_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id)
);

-- Validated: Customers that passed validation
CREATE TABLE validated.validated_customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    phone VARCHAR(50),
    address VARCHAR(500),
    validated_at DATETIME2 DEFAULT GETDATE()
);

-- Final: Production customer data
CREATE TABLE final.customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    phone VARCHAR(50),
    address VARCHAR(500),
    source_system VARCHAR(100),
    first_synced_at DATETIME2 NOT NULL,
    last_synced_at DATETIME2 NOT NULL,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    INDEX idx_external_id (external_id),
    INDEX idx_email (email)
);
```

#### Day 2-3: Build Mock External API

**Tasks:**
- Create Express.js mock CRM API with customer endpoints
- Generate 100 fake customers using faker library
- Implement pagination support
- Add random delays and error rates to simulate real-world conditions
- Test API with Postman or curl

**Cursor Prompt:**
```
Create a simple Express.js API that serves customer data:
- GET /api/customers - returns paginated list of mock customers
- GET /api/customers/:id - returns single customer
- Include pagination (page, size parameters)
- Return JSON with: id, name, email, phone, address, lastUpdated
- Generate 100 fake customers using faker library
- Add random delays (100-500ms) to simulate real API
- Add occasional errors (5% failure rate) to test error handling
```

#### Day 3-4: Build Integration Service

**Tasks:**
- Create CrmApiClient using RestTemplate
- Implement CustomerIntegrationService to orchestrate sync
- Create SyncJobService to track job status
- Build REST controller endpoint to trigger manual sync
- Implement error handling and logging
- Test end-to-end: trigger sync → verify data in staging table

**Cursor Prompt:**
```
Create a CustomerIntegrationService that:
1. Fetches customers from the mock CRM API (http://localhost:3001/api/customers)
2. Saves raw data to staging.raw_customers table
3. Creates a SyncJob record to track the operation
4. Uses RestTemplate to call the external API
5. Handles pagination (fetch all pages)
6. Includes error handling and logging
7. Updates the SyncJob with final status and counts
```

**Example Controller Endpoint:**
```java
@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {
    
    @PostMapping("/sync/customers")
    public ResponseEntity<SyncJobDTO> syncCustomers() {
        SyncJob job = customerIntegrationService.syncCustomers();
        return ResponseEntity.ok(mapToDTO(job));
    }
    
    @GetMapping("/jobs")
    public ResponseEntity<List<SyncJobDTO>> getRecentJobs() {
        return ResponseEntity.ok(syncJobService.getRecentJobs(20));
    }
}
```

### Phase 2: Data Transformation & Validation (Week 2)

#### Day 5-6: Build Transformation Pipeline

**Tasks:**
- Create CustomerTransformationService
- Parse raw JSON from staging table
- Apply transformations (normalize phone, lowercase email, clean address)
- Implement validation rules (email format, required fields)
- Save valid records to validated schema
- Log validation errors to audit.sync_errors table

**Cursor Prompt:**
```
Create a CustomerTransformationService that:
1. Reads from staging.raw_customers
2. Parses the raw JSON data
3. Applies transformations:
   - Normalize phone numbers (remove formatting)
   - Standardize email to lowercase
   - Clean address fields
   - Handle null values
4. Validates data:
   - Email format is valid
   - Name is not empty
   - External ID exists
5. Saves valid records to validated.validated_customers
6. Logs validation errors to audit.sync_errors
7. Returns transformation statistics
```

#### Day 6-7: Build Data Loading Service

**Tasks:**
- Create CustomerLoadService with upsert logic
- Implement stored procedure for bulk MERGE operation
- Handle both INSERT (new records) and UPDATE (existing records)
- Track first_synced_at and last_synced_at timestamps
- Return statistics (records inserted, updated, failed)

**Cursor Prompt:**
```
Create a CustomerLoadService that:
1. Reads from validated.validated_customers
2. For each record, check if it exists in final.customers (by external_id)
3. If exists: UPDATE the record, set last_synced_at
4. If new: INSERT the record, set first_synced_at and last_synced_at
5. Use a stored procedure for efficient bulk upsert
6. Return counts of inserted vs updated records
```

**Stored Procedure:** `V2__upsert_procedure.sql`

```sql
CREATE PROCEDURE final.upsert_customers
AS
BEGIN
    MERGE final.customers AS target
    USING validated.validated_customers AS source
    ON target.external_id = source.external_id
    
    WHEN MATCHED THEN
        UPDATE SET
            name = source.name,
            email = source.email,
            phone = source.phone,
            address = source.address,
            last_synced_at = GETDATE(),
            updated_at = GETDATE()
    
    WHEN NOT MATCHED THEN
        INSERT (external_id, name, email, phone, address, first_synced_at, last_synced_at)
        VALUES (source.external_id, source.name, source.email, source.phone, source.address, GETDATE(), GETDATE());
END;
```

### Phase 3: AWS Integration (Week 3)

#### Day 8-9: Integrate SQS Message Queue

**Tasks:**
- Add AWS SDK dependencies to project
- Configure SQS client to connect to LocalStack
- Create queue named 'data-sync-jobs'
- Implement message sender to enqueue sync requests
- Create message listener to poll queue and trigger syncs
- Add retry logic and dead-letter queue for failures
- Test async workflow: send message → listener processes → data synced

**Cursor Prompt:**
```
Create an SQS configuration that:
1. Connects to LocalStack (http://localhost:4566)
2. Creates a queue named 'data-sync-jobs'
3. Provides methods to:
   - Send sync job messages (sourceName, syncType)
   - Receive messages from the queue
   - Delete processed messages
4. Include retry logic for failed sends
```

**Message Listener Prompt:**
```
Create a SyncJobMessageListener that:
1. Polls the SQS queue every 10 seconds
2. When a message arrives, triggers the appropriate integration service
3. Updates the sync job status
4. Deletes the message on success
5. Moves message to DLQ on repeated failures
```

### Phase 4: Frontend Dashboard (Week 3-4)

#### Day 10-12: Build React Dashboard

**Tasks:**
- Create Next.js application with TypeScript
- Build SyncJobTable component to display recent jobs
- Create TriggerSyncButton to initiate new syncs
- Implement SyncJobDetails page showing errors and logs
- Add SyncMetricsChart using Recharts for visualization
- Use React Query for API state management and polling
- Style with TailwindCSS for professional appearance

**Cursor Prompt:**
```
Create a Next.js 14 app with TypeScript that includes:
- Dashboard page showing recent sync jobs (table with status, timestamps, record counts)
- Detail page for each sync job showing errors
- Button to trigger a new sync
- Real-time status updates (polling every 5 seconds)
- Charts showing sync success rate over time (using Recharts)
- Use TailwindCSS for styling
- Use React Query for data fetching
```

**Key Components:**
- `SyncJobTable.tsx` - displays jobs with status badges
- `TriggerSyncButton.tsx` - button to start new sync
- `SyncJobDetails.tsx` - shows errors and detailed logs
- `SyncMetricsChart.tsx` - visualizes sync performance

### Phase 5: Testing & Polish (Week 4)

#### Day 13-14: Add Comprehensive Testing

**Unit Tests with JUnit & Mockito:**
```
Create unit tests for CustomerIntegrationService:
- Test successful sync with mocked API response
- Test handling of API errors
- Test pagination logic
- Test sync job status updates
- Mock all external dependencies (RestTemplate, repositories)
```

**Integration Tests:**
```
Create integration tests that:
- Use @SpringBootTest and test containers for SQL Server
- Test the full sync flow: fetch -> transform -> validate -> load
- Verify data in all schemas (staging, validated, final)
- Test error scenarios and rollback behavior
```

**E2E Tests with Playwright:**
```
Create Playwright tests that:
- Navigate to the dashboard
- Trigger a sync job
- Wait for completion
- Verify the job appears in the table with 'COMPLETED' status
- Click into the job details
- Verify record counts are displayed
```

---

## Database Schema Design

### Schema Organization

The database is organized into four distinct schemas, each serving a specific purpose in the data integration pipeline:

### 1. Staging Schema (staging.*)

**Purpose:** Store raw, unmodified data from external sources

- Contains raw JSON payloads exactly as received from APIs
- Linked to sync jobs for full audit trail
- Allows data replay if transformation logic changes
- Temporary storage - can be purged after successful processing

### 2. Validated Schema (validated.*)

**Purpose:** Store cleaned and validated data ready for final loading

- Structured tables matching final schema design
- Only contains records that passed all validation rules
- Intermediate checkpoint before production data
- Enables data quality review before final load

### 3. Final Schema (final.*)

**Purpose:** Production-ready data available for reporting and analytics

- Optimized for query performance with appropriate indexes
- Contains historical tracking (first_synced_at, last_synced_at)
- Supports upsert operations (insert new, update existing)
- Source of truth for downstream applications

### 4. Audit Schema (audit.*)

**Purpose:** Track all integration activity and maintain data lineage

- sync_jobs table tracks every integration run
- sync_errors table logs all validation and processing errors
- Enables troubleshooting and data quality analysis
- Permanent records for compliance and auditing

---

## Key Features & Capabilities

### Data Integration Features

- **Multi-Source Support**: Connect to REST APIs, databases, file systems
- **Pagination Handling**: Automatically fetch all pages from paginated APIs
- **Error Recovery**: Retry logic, dead-letter queues, partial success handling
- **Incremental Sync**: Support for both full and incremental data loads
- **Scheduled Jobs**: Cron-based scheduling for automated syncs

### Data Quality Features

- **Validation Rules**: Configurable validation for data integrity
- **Transformation Pipeline**: Clean, normalize, and enrich data
- **Duplicate Detection**: Identify and handle duplicate records
- **Error Logging**: Detailed error messages with failed record context

### Observability Features

- **Real-Time Dashboard**: Live sync status and progress tracking
- **Metrics & Charts**: Success rates, throughput, error trends
- **Audit Trail**: Complete history of all data movements
- **Performance Monitoring**: Track sync duration and resource usage

### Security Features

- **Authentication**: JWT-based authentication for API access
- **Authorization**: Role-based access control (RBAC)
- **Secrets Management**: External API credentials stored securely
- **CORS Configuration**: Proper cross-origin resource sharing setup

---

## Testing Strategy

### Unit Testing

**Framework:** JUnit 5 + Mockito

- Test all service layer methods in isolation
- Mock external dependencies (repositories, API clients, queues)
- Test business logic edge cases and error conditions
- Verify validation rules and transformation logic
- Target >80% code coverage

### Integration Testing

**Framework:** Spring Boot Test + Testcontainers

- Test full integration flow with real database
- Use Testcontainers for SQL Server instance
- Verify data persists correctly across all schemas
- Test transaction rollback on errors
- Validate stored procedure execution

### End-to-End Testing

**Framework:** Playwright

- Test complete user workflows through UI
- Trigger sync from dashboard, verify status updates
- Navigate to job details, verify error display
- Test responsive design and accessibility
- Verify charts render correctly with data

### Frontend Testing

**Framework:** Jest + React Testing Library

- Test React components in isolation
- Mock API responses with MSW (Mock Service Worker)
- Test user interactions and state updates
- Verify error handling and loading states

---

## Development Setup Guide

### Prerequisites

- Java 17 or 21 JDK installed
- Maven 3.8+ or Gradle 8+
- Node.js 18+ and npm
- Docker Desktop installed and running
- Cursor IDE (recommended) or IntelliJ IDEA / VS Code
- Azure Data Studio or DBeaver for database management

### Initial Setup Steps

#### 1. Create Project Directory

```bash
mkdir data-integration-platform
cd data-integration-platform
```

#### 2. Initialize Spring Boot Backend

Use Cursor to generate Spring Boot project with required dependencies, or use Spring Initializr manually.

#### 3. Start Infrastructure

```bash
docker-compose up -d
```

#### 4. Run Backend Application

```bash
cd backend
./mvnw spring-boot:run
```

#### 5. Start Mock API

```bash
cd mock-apis/crm-api
npm install
npm start
```

#### 6. Start Frontend Dashboard

```bash
cd frontend
npm install
npm run dev
```

### Verify Setup

- Backend API: http://localhost:8080/health
- Frontend Dashboard: http://localhost:3000
- Mock CRM API: http://localhost:3001/api/customers
- SQL Server: localhost:1433 (username: sa, password: YourStrong@Passw0rd)

---

## Claude Code CLI Development Guide

### Installation & Setup

**Install Claude Code:**
```bash
# Install via npm (requires Node.js 18+)
npm install -g @anthropic-ai/claude-code

# Verify installation
claude --version
```

**Requirements:**
- Active Claude Pro ($20/month) or Claude Max subscription
- Node.js 18 or higher
- Git installed (for version control features)

### How to Use Claude Code

Claude Code is a terminal-based agentic coding assistant. Here's how to interact with it:

#### Starting a Session

**Start a new session:**
```bash
# Navigate to your project directory
cd ~/data-integration-platform

# Start Claude Code
claude
```

**Resume your last session:**
```bash
claude -c
```

**Start with a specific prompt:**
```bash
claude "Create a Spring Boot project with JPA and SQL Server dependencies"
```

#### During a Session

Once in a Claude Code session, you can:

**Chat naturally:**
```
> Create a SyncJob entity with JPA annotations
```

**Reference files with @:**
```
> Review @src/main/java/com/dataplatform/model/SyncJob.java for issues
```

**Use slash commands:**
```
/help              # Show all available commands
/init              # Create CLAUDE.md project file
/clear             # Clear conversation context (use often!)
/model opus        # Switch to Claude Opus
/model sonnet      # Switch to Claude Sonnet
/review            # Review code for issues
/explain           # Explain code architecture
```

**Stop Claude (important!):**
- Press `Escape` to stop Claude's current action
- Use `Ctrl+C` to exit the session entirely

**View previous messages:**
- Press `Escape` twice to see message history and jump to any previous message

### Project Configuration with CLAUDE.md

Create a `CLAUDE.md` file in your project root to give Claude persistent context:

```bash
# Let Claude help you create it
claude
> /init

# Or create manually
```

**Example CLAUDE.md for this project:**
```markdown
# Data Integration Platform

## Project Overview
Enterprise data integration system using Spring Boot, SQL Server, React, and AWS.
Demonstrates full-stack development skills for financial services applications.

## Technology Stack
- Backend: Java 17, Spring Boot 3.x, Spring Data JPA, Spring Security
- Database: SQL Server with Flyway migrations
- Frontend: React 18, TypeScript, Next.js, TailwindCSS
- Cloud: AWS (SQS, S3) via LocalStack for local development
- Testing: JUnit 5, Mockito, Jest, Playwright

## Architecture
- Four-schema database design: staging → validated → final + audit
- ETL pipeline: fetch → transform → validate → load
- Async processing via SQS message queues
- REST APIs + GraphQL for frontend

## Coding Standards
- Use constructor injection, not field injection
- All services use SLF4J for logging
- DTOs in dto package, entities in model package
- Use ResponseEntity for all controller methods
- Follow REST conventions (GET/POST/PUT/DELETE)
- Validation errors return 400 with detailed messages
- Use @Transactional for database operations
- Test coverage minimum 80%

## File Organization
- Entities: src/main/java/com/dataplatform/model/
- Services: src/main/java/com/dataplatform/service/
- Controllers: src/main/java/com/dataplatform/controller/
- DTOs: src/main/java/com/dataplatform/dto/
- Tests: src/test/java/com/dataplatform/

## Database Conventions
- All tables use BIGINT IDENTITY primary keys
- Use VARCHAR(MAX) for JSON storage
- Include created_at and updated_at timestamps
- Add indexes for foreign keys and commonly queried fields
- Schema-qualify all table names (staging.raw_customers)

## Important Commands
# Start infrastructure
docker-compose up -d

# Run backend
cd backend && ./mvnw spring-boot:run

# Run tests
./mvnw test

# Run frontend
cd frontend && npm run dev

## Known Issues
- SQL Server Express has 10GB database size limit
- LocalStack requires restart after machine sleep
```

### Effective Prompting for Claude Code

**Good prompts are specific and actionable:**

❌ **Bad:**
```
> Help me with the sync service
```

✅ **Good:**
```
> Create a CustomerIntegrationService that:
- Fetches customers from http://localhost:3001/api/customers
- Handles pagination automatically
- Saves raw JSON to staging.raw_customers table  
- Creates SyncJob records to track status
- Uses @Transactional for database operations
- Includes error handling and SLF4J logging
```

**Provide context efficiently:**
```
> I'm building the transformation layer. Read @CLAUDE.md for context.
> Create a CustomerTransformationService that validates and cleans data
> from staging.raw_customers and saves to validated.validated_customers
```

**Iterate naturally:**
```
> Create the transformation service
[Claude generates code]

> Add email validation using regex pattern
[Claude updates code]

> Now add unit tests with Mockito
[Claude creates test file]
```

### Advanced Features

#### Custom Commands

Create reusable commands in `.claude/commands/`:

```bash
mkdir -p .claude/commands

# Create a custom review command
cat > .claude/commands/security-review.md << 'EOF'
# Security Review Command

Review the following aspects:
1. SQL injection vulnerabilities
2. Authentication/authorization issues  
3. Sensitive data exposure
4. Input validation gaps
5. Error handling that leaks information
EOF
```

Use it:
```
> /security-review @src/main/java/com/dataplatform/controller/
```

#### Skills (Project-Specific Knowledge)

Create a skill for complex, repeated tasks:

```bash
mkdir -p .claude/skills/spring-boot-service

cat > .claude/skills/spring-boot-service/SKILL.md << 'EOF'
---
name: spring-boot-service
description: Create Spring Boot service classes following project conventions
---

# Spring Boot Service Skill

When creating service classes:

1. Use constructor injection with @RequiredArgsConstructor
2. Add @Service annotation
3. Include @Slf4j for logging
4. Use @Transactional for database operations
5. Create corresponding DTO classes
6. Follow repository naming: <Entity>Repository
7. Add JavaDoc comments
8. Handle exceptions with custom exception classes

## Example Structure:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ExampleService {
    private final ExampleRepository repository;
    
    @Transactional
    public ExampleDTO create(ExampleDTO dto) {
        log.info("Creating example: {}", dto);
        // implementation
    }
}
```
EOF
```

Claude automatically uses this skill when you ask it to create services.

#### Background Tasks

Run long-running tasks in the background:

```
> Run all tests in the background
> Start the Spring Boot application in the background
> Check background tasks
```

### Best Practices for This Project

#### 1. Use /clear Frequently

Clear context when switching between major features:
```
> /clear
> Now let's work on the frontend dashboard
```

#### 2. Generate Complete Features at Once

```
> Create the complete sync job feature:
- SyncJob entity with JPA annotations
- SyncJobRepository with custom queries  
- SyncJobService with CRUD operations
- SyncJobController with REST endpoints
- SyncJobDTO for API responses
- Unit tests with JUnit and Mockito
Follow all conventions in @CLAUDE.md
```

#### 3. Let Claude Handle File Creation

Don't manually create files - let Claude do it:
```
> Create the Flyway migration V1__initial_schema.sql in src/main/resources/db/migration/
> Include all four schemas: staging, validated, final, audit
```

#### 4. Use @-mentions for Context

```
> Review @src/main/java/com/dataplatform/service/CustomerIntegrationService.java
> Add error handling similar to @src/main/java/com/dataplatform/service/SyncJobService.java
```

#### 5. Test as You Go

```
> Create integration tests for @src/main/java/com/dataplatform/service/CustomerLoadService.java
> Use @SpringBootTest and Testcontainers for SQL Server
```

#### 6. Commit Regularly

Claude can help with git:
```
> Create a git commit for the transformation pipeline feature
> Review git diff and suggest commit message
```

### Daily Workflow with Claude Code

#### Morning Session (9:00 AM - 11:00 AM)

```bash
# Start your day
cd ~/data-integration-platform
claude

> /clear
> Read @CLAUDE.md and @PROJECT_SPEC.md
> Today we're implementing Phase 2: Data Transformation
> Create the CustomerTransformationService following all conventions
```

Work for 1-2 hours, letting Claude generate:
- Service classes
- Repository interfaces  
- DTOs
- Configuration files
- Initial tests

#### Afternoon Session (1:00 PM - 3:00 PM)

```bash
# Resume your session
claude -c

> Review the transformation service we created this morning
> Add validation rules for email format and phone numbers
> Create comprehensive unit tests
```

#### End of Day

```bash
> Review all changes made today
> Suggest improvements to the transformation logic
> Create a git commit summarizing today's work
> Update @CLAUDE.md with any new conventions we established
```

### Troubleshooting Common Issues

**Problem: Claude can't find files**
```
> /ls src/main/java/com/dataplatform/
> Show me the directory structure
```

**Problem: Code doesn't compile**
```
> Run mvn clean compile and show me the errors
> Fix all compilation errors in the service layer
```

**Problem: Tests failing**
```
> Run tests and analyze failures
> Fix the CustomerTransformationServiceTest failures
```

**Problem: Need to switch models**
```
> /model opus    # For complex architecture decisions
> /model sonnet  # For faster iteration on simple tasks
```

### Context Management

Claude Code maintains context automatically, but you can optimize it:

**Good practices:**
- Use `/clear` when switching major features
- Reference `@CLAUDE.md` at session start
- Use `@file` mentions instead of copying code
- Let Claude read error messages from command output

**Avoid:**
- Pasting large code blocks (use @file instead)
- Keeping conversation going for 50+ messages without `/clear`
- Mixing multiple unrelated tasks in one session

---

## Next Steps & Extensions

After completing the core platform, consider these extensions:

### Additional Data Sources

- Add ERP API integration for order data
- Implement accounting API for financial transactions
- Support CSV/Excel file uploads via S3

### Advanced Features

- Implement data deduplication algorithms
- Add data lineage tracking and visualization
- Create configurable transformation rules via UI
- Implement webhook notifications for sync completion
- Add data export capabilities (CSV, Excel, JSON)

### Performance Optimization

- Implement parallel processing for large datasets
- Add caching layer with Redis
- Optimize database queries with execution plans
- Implement connection pooling tuning

### Production Readiness

- Set up CI/CD pipeline with GitHub Actions
- Deploy to AWS (ECS, RDS, actual SQS)
- Configure monitoring with CloudWatch or DataDog
- Add structured logging with ELK stack
- Implement health checks and alerting

---

## GraphQL Extension

### Overview

GraphQL is a powerful alternative to REST APIs that allows clients to request exactly the data they need in a single query. Adding GraphQL to the Data Integration Platform demonstrates understanding of modern API design patterns and when to use different tools for different problems.

### What is GraphQL?

GraphQL is a **query language for APIs** and a **runtime for executing those queries**. Created by Facebook and open-sourced in 2015, it addresses common REST API limitations:

**Key Differences from REST:**

| Aspect | REST | GraphQL |
|--------|------|---------|
| **Endpoints** | Multiple (one per resource) | Single endpoint |
| **Data fetching** | Fixed response structure | Client specifies fields |
| **Over-fetching** | Common (get data you don't need) | Eliminated |
| **Under-fetching** | Common (need multiple requests) | Eliminated |
| **Versioning** | Often need v1, v2, etc. | No versioning needed |
| **Documentation** | Often out of date | Self-documenting (schema) |

### Why Use GraphQL in This Project?

#### 1. Dashboard Complexity

The dashboard requires data from multiple sources:
- Sync job list with counts
- Each job's error details
- Metrics and charts
- Configuration settings

**Current REST Approach (Multiple Calls):**
```javascript
// Fetch sync jobs
const jobs = await fetch('/api/integrations/jobs');

// For each job, fetch errors
const errors = await fetch(`/api/integrations/jobs/${jobId}/errors`);

// Fetch metrics separately
const metrics = await fetch('/api/integrations/metrics');
```

**GraphQL Approach (Single Query):**
```graphql
query DashboardData {
  recentSyncJobs(limit: 20) {
    id
    sourceName
    status
    startTime
    endTime
    recordsProcessed
    recordsFailed
    errors {
      errorMessage
      failedRecord
      occurredAt
    }
  }
  
  syncMetrics(period: LAST_30_DAYS) {
    successRate
    avgDuration
    totalRecords
    dailyStats {
      date
      syncsCompleted
      syncsFailed
    }
  }
}
```

#### 2. Real-Time Updates

GraphQL subscriptions enable live updates without polling:

```graphql
subscription WatchSyncJob($jobId: ID!) {
  syncJobUpdated(id: $jobId) {
    status
    recordsProcessed
    recordsFailed
    currentPhase  # FETCHING, TRANSFORMING, LOADING
  }
}
```

#### 3. Flexible Filtering

With REST, you'd need many endpoints for different filters. With GraphQL, one query handles all cases:

```graphql
query FilteredJobs(
  $status: SyncStatus
  $source: String
  $dateRange: DateRange
) {
  syncJobs(
    filter: {
      status: $status
      sourceName: $source
      startTime: $dateRange
    }
    orderBy: START_TIME_DESC
  ) {
    id
    sourceName
    status
    recordsProcessed
  }
}
```

### Implementation Guide

#### Step 1: Add Dependencies

**Maven (pom.xml):**
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

**Gradle (build.gradle):**
```gradle
implementation 'org.springframework.boot:spring-boot-starter-graphql'
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

#### Step 2: Define GraphQL Schema

**File:** `src/main/resources/graphql/schema.graphqls`

```graphql
type Query {
  syncJob(id: ID!): SyncJob
  syncJobs(
    limit: Int = 20
    offset: Int = 0
    filter: SyncJobFilter
    orderBy: SyncJobOrderBy
  ): [SyncJob!]!
  
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
  syncType: SyncType!
  status: SyncStatus!
  startTime: DateTime!
  endTime: DateTime
  recordsProcessed: Int!
  recordsFailed: Int!
  duration: Int  # Computed field
  successRate: Float  # Computed field
  
  errors(limit: Int = 10): [SyncError!]!
  stagingRecords(limit: Int = 10): [StagingRecord!]!
  validationStats: ValidationStats
}

type SyncError {
  id: ID!
  errorType: String
  errorMessage: String!
  failedRecord: String
  occurredAt: DateTime!
}

type ValidationStats {
  totalRecords: Int!
  passedValidation: Int!
  failedValidation: Int!
  topErrors: [ErrorCount!]!
}

type ErrorCount {
  errorType: String!
  count: Int!
}

type SyncMetrics {
  last24Hours: MetricsSummary!
  last30Days: MetricsSummary!
}

type MetricsSummary {
  totalSyncs: Int!
  successRate: Float!
  avgDuration: Float!
  totalRecords: Int!
  dailyStats: [DailyStats!]!
}

type DailyStats {
  date: Date!
  syncsCompleted: Int!
  syncsFailed: Int!
}

type StagingRecord {
  id: ID!
  externalId: String
  rawData: String!
  receivedAt: DateTime!
}

enum SyncStatus {
  RUNNING
  COMPLETED
  FAILED
}

enum SyncType {
  FULL
  INCREMENTAL
}

enum MetricsPeriod {
  LAST_24_HOURS
  LAST_7_DAYS
  LAST_30_DAYS
}

enum SyncJobOrderBy {
  START_TIME_ASC
  START_TIME_DESC
  RECORDS_PROCESSED_DESC
}

input SyncJobFilter {
  status: SyncStatus
  sourceName: String
  startTime: DateRange
}

input DateRange {
  from: DateTime!
  to: DateTime!
}

input TriggerSyncInput {
  sourceName: String!
  syncType: SyncType = FULL
}

scalar DateTime
scalar Date
```

#### Step 3: Create GraphQL Resolvers

**File:** `src/main/java/com/dataplatform/graphql/SyncJobResolver.java`

```java
package com.dataplatform.graphql;

import com.dataplatform.dto.*;
import com.dataplatform.model.SyncJob;
import com.dataplatform.model.SyncError;
import com.dataplatform.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SyncJobResolver {
    
    private final SyncJobService syncJobService;
    private final SyncErrorService syncErrorService;
    private final MetricsService metricsService;
    
    // Query: Fetch single job
    @QueryMapping
    public SyncJob syncJob(@Argument Long id) {
        return syncJobService.findById(id);
    }
    
    // Query: Fetch multiple jobs with filtering
    @QueryMapping
    public List<SyncJob> syncJobs(
        @Argument Integer limit,
        @Argument Integer offset,
        @Argument SyncJobFilter filter,
        @Argument SyncJobOrderBy orderBy
    ) {
        return syncJobService.findAll(limit, offset, filter, orderBy);
    }
    
    // Query: Get metrics
    @QueryMapping
    public SyncMetrics syncMetrics(@Argument MetricsPeriod period) {
        return metricsService.getMetrics(period);
    }
    
    // Mutation: Trigger new sync
    @MutationMapping
    public SyncJob triggerSync(@Argument TriggerSyncInput input) {
        return syncJobService.triggerSync(
            input.getSourceName(), 
            input.getSyncType()
        );
    }
    
    // Mutation: Cancel running sync
    @MutationMapping
    public SyncJob cancelSync(@Argument Long jobId) {
        return syncJobService.cancelSync(jobId);
    }
    
    // Subscription: Watch job progress in real-time
    @SubscriptionMapping
    public Flux<SyncJob> syncJobUpdated(@Argument Long id) {
        // Emit updates every 2 seconds
        return syncJobService.watchJob(id)
            .delayElements(Duration.ofSeconds(2));
    }
    
    // Field resolver: Get errors for a job
    @SchemaMapping(typeName = "SyncJob", field = "errors")
    public List<SyncError> errors(SyncJob syncJob, @Argument Integer limit) {
        return syncErrorService.findByJobId(syncJob.getId(), limit);
    }
    
    // Field resolver: Get staging records
    @SchemaMapping(typeName = "SyncJob", field = "stagingRecords")
    public List<StagingRecord> stagingRecords(SyncJob syncJob, @Argument Integer limit) {
        return syncJobService.getStagingRecords(syncJob.getId(), limit);
    }
    
    // Field resolver: Get validation statistics
    @SchemaMapping(typeName = "SyncJob", field = "validationStats")
    public ValidationStats validationStats(SyncJob syncJob) {
        return syncJobService.getValidationStats(syncJob.getId());
    }
    
    // Field resolver: Computed duration in seconds
    @SchemaMapping(typeName = "SyncJob", field = "duration")
    public Integer duration(SyncJob syncJob) {
        if (syncJob.getEndTime() == null) return null;
        return (int) Duration.between(
            syncJob.getStartTime(), 
            syncJob.getEndTime()
        ).toSeconds();
    }
    
    // Field resolver: Computed success rate
    @SchemaMapping(typeName = "SyncJob", field = "successRate")
    public Float successRate(SyncJob syncJob) {
        int total = syncJob.getRecordsProcessed() + syncJob.getRecordsFailed();
        if (total == 0) return null;
        return (float) syncJob.getRecordsProcessed() / total * 100;
    }
}
```

#### Step 4: Configure GraphQL Settings

**File:** `application.yml`

```yaml
spring:
  graphql:
    graphiql:
      enabled: true  # Enable GraphiQL UI for testing
      path: /graphiql
    path: /graphql
    websocket:
      path: /graphql  # WebSocket endpoint for subscriptions
```

#### Step 5: Frontend Integration with Apollo Client

**Install Dependencies:**
```bash
npm install @apollo/client graphql graphql-ws
```

**Setup Apollo Client:**

**File:** `src/lib/apollo-client.ts`

```typescript
import { ApolloClient, InMemoryCache, split, HttpLink } from '@apollo/client';
import { GraphQLWsLink } from '@apollo/client/link/subscriptions';
import { getMainDefinition } from '@apollo/client/utilities';
import { createClient } from 'graphql-ws';

const httpLink = new HttpLink({
  uri: 'http://localhost:8080/graphql',
});

const wsLink = new GraphQLWsLink(createClient({
  url: 'ws://localhost:8080/graphql',
}));

// Split between HTTP (queries/mutations) and WebSocket (subscriptions)
const splitLink = split(
  ({ query }) => {
    const definition = getMainDefinition(query);
    return (
      definition.kind === 'OperationDefinition' &&
      definition.operation === 'subscription'
    );
  },
  wsLink,
  httpLink,
);

export const apolloClient = new ApolloClient({
  link: splitLink,
  cache: new InMemoryCache(),
});
```

**Wrap App with Apollo Provider:**

**File:** `src/app/layout.tsx`

```typescript
'use client';

import { ApolloProvider } from '@apollo/client';
import { apolloClient } from '@/lib/apollo-client';

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <ApolloProvider client={apolloClient}>
          {children}
        </ApolloProvider>
      </body>
    </html>
  );
}
```

**Create Dashboard Component:**

**File:** `src/components/SyncJobDashboard.tsx`

```typescript
'use client';

import { useQuery, useMutation, useSubscription } from '@apollo/client';
import { gql } from '@apollo/client';

const DASHBOARD_QUERY = gql`
  query Dashboard {
    syncJobs(limit: 20) {
      id
      sourceName
      status
      startTime
      endTime
      recordsProcessed
      recordsFailed
      duration
      successRate
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
        avgDuration
      }
      last30Days {
        successRate
        totalRecords
      }
    }
  }
`;

const TRIGGER_SYNC = gql`
  mutation TriggerSync($source: String!) {
    triggerSync(input: { sourceName: $source }) {
      id
      status
      startTime
    }
  }
`;

const WATCH_SYNC = gql`
  subscription WatchSync($jobId: ID!) {
    syncJobUpdated(id: $jobId) {
      id
      status
      recordsProcessed
      recordsFailed
    }
  }
`;

export function SyncJobDashboard() {
  const { data, loading, error, refetch } = useQuery(DASHBOARD_QUERY);
  
  const [triggerSync, { loading: triggering }] = useMutation(TRIGGER_SYNC, {
    refetchQueries: ['Dashboard'],
  });
  
  if (loading) return <div>Loading dashboard...</div>;
  if (error) return <div>Error: {error.message}</div>;
  
  const handleTriggerSync = async (source: string) => {
    try {
      await triggerSync({ variables: { source } });
    } catch (err) {
      console.error('Failed to trigger sync:', err);
    }
  };
  
  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Data Integration Dashboard</h1>
        <button 
          onClick={() => handleTriggerSync('CRM')}
          disabled={triggering}
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {triggering ? 'Triggering...' : 'Trigger CRM Sync'}
        </button>
      </div>
      
      {/* Metrics Summary */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <MetricCard 
          title="24h Syncs"
          value={data.syncMetrics.last24Hours.totalSyncs}
        />
        <MetricCard 
          title="24h Success Rate"
          value={`${data.syncMetrics.last24Hours.successRate.toFixed(1)}%`}
        />
        <MetricCard 
          title="30d Total Records"
          value={data.syncMetrics.last30Days.totalRecords.toLocaleString()}
        />
      </div>
      
      {/* Sync Jobs Table */}
      <div className="bg-white rounded-lg shadow">
        <table className="min-w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Source
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Records
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Success Rate
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                Duration
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {data.syncJobs.map(job => (
              <SyncJobRow key={job.id} job={job} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function SyncJobRow({ job }) {
  // Subscribe to real-time updates for running jobs
  const { data: liveData } = useSubscription(WATCH_SYNC, {
    variables: { jobId: job.id },
    skip: job.status !== 'RUNNING',
  });
  
  const currentJob = liveData?.syncJobUpdated || job;
  
  return (
    <tr className={currentJob.status === 'RUNNING' ? 'bg-blue-50' : ''}>
      <td className="px-6 py-4">{currentJob.sourceName}</td>
      <td className="px-6 py-4">
        <StatusBadge status={currentJob.status} />
      </td>
      <td className="px-6 py-4">
        {currentJob.recordsProcessed} / {currentJob.recordsProcessed + currentJob.recordsFailed}
      </td>
      <td className="px-6 py-4">
        {currentJob.successRate?.toFixed(1)}%
      </td>
      <td className="px-6 py-4">
        {currentJob.duration ? `${currentJob.duration}s` : '-'}
      </td>
    </tr>
  );
}
```

### Testing GraphQL

#### Using GraphiQL (Built-in UI)

Navigate to `http://localhost:8080/graphiql` and test queries:

```graphql
# Test Query
query {
  syncJobs(limit: 5) {
    id
    sourceName
    status
    recordsProcessed
  }
}

# Test Mutation
mutation {
  triggerSync(input: { sourceName: "CRM" }) {
    id
    status
    startTime
  }
}

# Test Subscription
subscription {
  syncJobUpdated(id: "123") {
    status
    recordsProcessed
  }
}
```

#### Integration Tests

**File:** `src/test/java/com/dataplatform/graphql/SyncJobResolverTest.java`

```java
@SpringBootTest
@AutoConfigureGraphQlTester
class SyncJobResolverTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @MockBean
    private SyncJobService syncJobService;
    
    @Test
    void shouldFetchSyncJobs() {
        // Given
        List<SyncJob> mockJobs = List.of(
            createMockJob(1L, "CRM", SyncStatus.COMPLETED)
        );
        when(syncJobService.findAll(any(), any(), any(), any()))
            .thenReturn(mockJobs);
        
        // When/Then
        graphQlTester.documentName("syncJobs")
            .variable("limit", 20)
            .execute()
            .path("syncJobs")
            .entityList(SyncJob.class)
            .hasSize(1)
            .path("syncJobs[0].sourceName")
            .entity(String.class)
            .isEqualTo("CRM");
    }
    
    @Test
    void shouldTriggerSync() {
        // Given
        SyncJob mockJob = createMockJob(1L, "CRM", SyncStatus.RUNNING);
        when(syncJobService.triggerSync(anyString(), any()))
            .thenReturn(mockJob);
        
        // When/Then
        graphQlTester.documentName("triggerSync")
            .variable("input", Map.of("sourceName", "CRM"))
            .execute()
            .path("triggerSync.status")
            .entity(String.class)
            .isEqualTo("RUNNING");
    }
}
```

### GraphQL vs REST: When to Use Each

**Use GraphQL for:**
- ✅ Dashboard queries (complex nested data)
- ✅ Job details page (related entities)
- ✅ Real-time sync monitoring (subscriptions)
- ✅ Analytics queries (flexible aggregations)
- ✅ Mobile apps (reduce bandwidth with precise queries)

**Use REST for:**
- ✅ File uploads (CSV/Excel imports)
- ✅ Simple CRUD operations
- ✅ Health checks and status endpoints
- ✅ Webhook callbacks from external systems
- ✅ Operations with well-defined, stable contracts

**Recommended Hybrid Approach:**
- Offer both REST and GraphQL APIs
- Use REST for actions: `POST /api/sync/trigger`
- Use GraphQL for queries: Dashboard and analytics
- Use WebSocket subscriptions for real-time updates

### Benefits Demonstrated

Implementing GraphQL extension shows:

1. **Modern API Design** - Understanding when to use different API patterns
2. **Frontend Performance** - Reducing network overhead and round trips
3. **Type Safety** - Leveraging strong typing across the stack
4. **Real-time Capabilities** - WebSocket subscriptions for live updates
5. **Developer Experience** - Self-documenting APIs and excellent tooling
6. **Full-Stack Thinking** - How backend choices affect frontend development

### Additional Resources

- **Spring for GraphQL Docs**: https://docs.spring.io/spring-graphql/reference/
- **Apollo Client Docs**: https://www.apollographql.com/docs/react/
- **GraphQL Spec**: https://spec.graphql.org/
- **GraphQL Best Practices**: https://graphql.org/learn/best-practices/

---

## Project Timeline with Claude Code CLI

### Realistic Timeline: 3-4 Weeks

With Claude Code CLI and smart usage patterns, you can complete this project in **3-4 weeks** (12-16 working days) without hitting rate limits.

### Daily Session Structure

**Morning Session (2-3 hours)**
- 9:00 AM: Start Claude Code session, review CLAUDE.md
- 9:15 AM - 11:00 AM: Generate code (use ~20-25 prompts)
  - Create entities, services, repositories
  - Generate configuration files
  - Build controller endpoints
- 11:00 AM - 12:00 PM: Independent work
  - Run generated code
  - Fix compilation errors
  - Test endpoints

**Afternoon Session (2-3 hours)**  
- 1:00 PM - 2:00 PM: Testing & debugging
  - Test morning's code
  - Identify issues
- 2:00 PM - 3:30 PM: Claude Code refinement (use ~15-20 prompts)
  - Fix issues from testing
  - Generate tests
  - Add documentation
- 3:30 PM - 5:00 PM: Independent work
  - Run tests
  - Commit to Git
  - Update documentation

**Evening (Optional - 1 hour)**
- Read documentation
- Watch tutorials
- No Claude Code usage (save for productive work)

### Week-by-Week Breakdown

#### Week 1: Backend Foundation (Phase 1)

**Monday - Setup (8 sessions)**
```bash
claude
> Read @PROJECT_SPEC.md for context
> Create Spring Boot project structure with Maven
> Generate application.yml with SQL Server configuration
> Create initial domain models: SyncJob, SyncError, Customer
> Generate docker-compose.yml for SQL Server and LocalStack
> Create initial Flyway migration with all schemas
> Start infrastructure and verify connectivity
> Create basic health check endpoint
```

**Tuesday - Integration Service (10 sessions)**
```bash
claude -c
> Create CrmApiClient using RestTemplate
> Implement CustomerIntegrationService with pagination
> Create SyncJobService to track job status  
> Build IntegrationController with REST endpoints
> Generate unit tests for all services
> Create integration test for end-to-end sync flow
```

**Wednesday - Mock API (6 sessions)**
```bash
claude
> /clear
> Create Express.js mock CRM API in mock-apis/crm-api
> Generate 100 fake customers with Faker
> Implement pagination and error simulation
> Test the API with curl
> Document API endpoints
```

**Thursday - Transformation (12 sessions)**
```bash
claude -c
> Create CustomerTransformationService
> Implement data validation rules
> Add transformation logic (normalize phone, clean addresses)
> Create validation error logging
> Generate unit tests with Mockito
> Test transformation pipeline end-to-end
```

**Friday - Data Loading (10 sessions)**
```bash
claude -c  
> Create CustomerLoadService with upsert logic
> Generate SQL Server stored procedure for MERGE operation
> Implement batch processing for performance
> Add error handling and transaction management
> Create comprehensive tests
> Test full pipeline: fetch → transform → validate → load
```

#### Week 2: AWS & Advanced Features (Phases 2-3)

**Monday - SQS Integration (12 sessions)**
```bash
claude
> /clear
> Configure AWS SDK for LocalStack
> Create SQS queue configuration
> Implement message sender for sync jobs
> Create message listener with polling
> Add retry logic and dead-letter queue
> Test async workflow
```

**Tuesday - Scheduler (8 sessions)**
```bash
claude -c
> Create SchedulerService with cron job support
> Implement scheduled sync triggers
> Add job monitoring and health checks
> Generate tests for scheduler
```

**Wednesday - Error Handling (10 sessions)**
```bash
claude -c
> Review all services for error handling gaps
> Create custom exception classes
> Implement global exception handler
> Add comprehensive error logging
> Test error scenarios
```

**Thursday - Metrics (10 sessions)**
```bash
claude -c
> Create MetricsService for sync statistics
> Implement aggregation queries
> Add performance tracking
> Generate reports endpoint
> Create tests
```

**Friday - Integration Testing (8 sessions)**
```bash
claude -c
> Create comprehensive integration test suite
> Use Testcontainers for SQL Server
> Test complete workflows
> Verify all schemas and data flow
> Document test coverage
```

#### Week 3: Frontend & Real-time Features (Phase 4)

**Monday - Next.js Setup (10 sessions)**
```bash
claude
> /clear
> Create Next.js 14 app with TypeScript
> Configure TailwindCSS
> Set up project structure
> Create layout and basic routing
> Add environment configuration
```

**Tuesday - Dashboard Components (15 sessions)**
```bash
claude -c
> Create SyncJobTable component
> Implement TriggerSyncButton
> Add status badges and formatting
> Create MetricsCard component
> Build navigation and layout
> Style with TailwindCSS
```

**Wednesday - Data Fetching (12 sessions)**
```bash
claude -c
> Set up React Query
> Create API client service
> Implement data fetching hooks
> Add loading and error states
> Test with real backend
```

**Thursday - Charts & Visualization (12 sessions)**
```bash
claude -c
> Install and configure Recharts
> Create SyncMetricsChart component
> Implement success rate chart
> Add daily statistics visualization
> Style charts for professional appearance
```

**Friday - Real-time Updates (10 sessions)**
```bash
claude -c
> Implement polling with React Query
> Add real-time status updates
> Create WebSocket connection (if using subscriptions)
> Test live updates during sync
> Polish UX and animations
```

#### Week 4: Testing, Polish & GraphQL (Phase 5 + Extension)

**Monday - Backend Testing (12 sessions)**
```bash
claude
> /clear
> Generate unit tests for all services
> Create integration tests for controllers
> Add tests for edge cases
> Achieve 80%+ code coverage
> Fix any test failures
```

**Tuesday - Frontend Testing (10 sessions)**
```bash
claude -c
> Create Jest tests for components
> Add React Testing Library tests
> Implement E2E tests with Playwright
> Test user workflows
> Fix any issues
```

**Wednesday - GraphQL Setup (12 sessions)**
```bash
claude -c
> Add GraphQL dependencies to Spring Boot
> Create schema.graphqls with all types
> Generate GraphQL resolvers
> Configure Apollo Client in frontend
> Create GraphQL queries and mutations
```

**Thursday - GraphQL Features (10 sessions)**
```bash
claude -c
> Implement subscriptions for real-time updates
> Add computed fields (duration, successRate)
> Create GraphQL integration tests
> Update frontend to use GraphQL
> Test GraphQL endpoints with GraphiQL
```

**Friday - Documentation & Polish (8 sessions)**
```bash
claude -c
> Generate comprehensive README.md
> Create API documentation
> Add inline code comments
> Update CLAUDE.md with lessons learned
> Prepare demo script
> Take screenshots for portfolio
```

### Usage Optimization Tips

#### Batch Your Requests

❌ **Inefficient (uses 5+ prompts):**
```
> Create Customer entity
[wait]
> Create SyncJob entity  
[wait]
> Create SyncError entity
[wait]
> Add JPA annotations
[wait]
> Add Lombok annotations
```

✅ **Efficient (uses 1 prompt):**
```
> Create three JPA entities with Lombok annotations:
1. Customer (id, externalId, name, email, phone, address)
2. SyncJob (id, sourceName, status, startTime, endTime, recordsProcessed, recordsFailed)
3. SyncError (id, syncJobId, errorMessage, failedRecord, occurredAt)

Include all JPA relationships, indexes, and proper column types.
Follow conventions in @CLAUDE.md
```

#### Front-Load Complex Work

Use more prompts early in the day for generation, fewer later for fixes:
- **Morning:** 20-25 prompts (heavy generation)
- **Afternoon:** 15-20 prompts (refinement)
- **Evening:** 0 prompts (independent work)

#### Strategic /clear Usage

Clear context when switching major features to save tokens:
```
# Finished backend work
> /clear

# Start frontend work  
> Read @CLAUDE.md for project context
> Now let's build the React dashboard
```

### Avoiding Rate Limits

**If you hit the limit:**

1. **Switch to independent work**
   - Run tests manually
   - Fix bugs yourself
   - Read documentation
   - Refactor code

2. **Take strategic breaks**
   - Take a walk (limits reset on rolling 5-hour window)
   - Plan next steps on paper
   - Review code you've written
   - Watch tutorials

3. **Use other resources**
   - Stack Overflow for specific errors
   - Spring Boot documentation
   - GitHub for examples
   - ChatGPT free tier for quick syntax questions

### Expected Daily Progress

**Good Days (Everything Works):**
- 3-4 complete features
- 10-15 test cases
- All code compiling
- ~35-40 prompts used

**Normal Days (Some Debugging):**
- 2-3 features  
- Some tests written
- Most code working
- ~40-45 prompts used

**Challenging Days (Complex Problems):**
- 1-2 features with iteration
- Complex debugging
- May hit limit, switch to independent work
- ~45-50 prompts used (may hit limit)

### Final Recommendation

**Target: 3 weeks core + 1 week polish**

- **Weeks 1-2:** Backend fully functional (demo-able)
- **Week 3:** Frontend dashboard (portfolio-ready)
- **Week 4:** Testing + GraphQL (interview impressive)

This timeline allows you to:
- Apply to jobs with working backend after 2 weeks
- Show visual demo after 3 weeks
- Discuss GraphQL expertise after 4 weeks

---

## Conclusion

This Multi-Source Data Integration Platform demonstrates proficiency across the full technology stack required for enterprise software development. By completing this project, you will have hands-on experience with:

- Spring Boot backend architecture and best practices
- Complex SQL Server database design and optimization
- Modern React frontend development with TypeScript
- AWS cloud services integration
- Comprehensive testing strategies
- Docker containerization and infrastructure as code
- Real-world enterprise integration patterns

The project is designed to be portfolio-ready and demonstrates the exact skills outlined in the Gen II Fund Services job description. It showcases your ability to build scalable, maintainable, production-grade software that solves real business problems.

**Start with Phase 1 to build a solid foundation, then iterate through each phase to create a comprehensive demonstration of your full stack development capabilities.**

---

## Quick Reference Commands

### Docker
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f sqlserver
```

### Backend
```bash
# Run Spring Boot app
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build
./mvnw clean package
```

### Frontend
```bash
# Install dependencies
npm install

# Run dev server
npm run dev

# Run tests
npm test

# Build for production
npm run build
```

### Database
```bash
# Connect to SQL Server
sqlcmd -S localhost,1433 -U sa -P YourStrong@Passw0rd

# Run migration manually
./mvnw flyway:migrate
```
