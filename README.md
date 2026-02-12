# Multi-Source Data Integration Platform

An enterprise-grade data integration system built with Spring Boot, SQL Server, React, and AWS. This project demonstrates proficiency in modern full-stack development and real-world integration patterns.

## 🎯 Project Overview

This platform simulates real-world data integration challenges faced by financial services and enterprise organizations. It showcases:

- **Backend**: Spring Boot 3.x, Spring Data JPA, Spring Security, Java 17+
- **Database**: SQL Server with multi-schema architecture (staging, validated, final, audit)
- **Frontend**: React 18, TypeScript, Next.js, TailwindCSS
- **Cloud**: AWS SQS for message queuing, S3 for file storage (via LocalStack)
- **Testing**: JUnit 5, Mockito, Jest, Playwright
- **Infrastructure**: Docker, Docker Compose, LocalStack

## 🏗️ Architecture

The system implements a robust ETL pipeline with four-schema design:

```
External APIs → Staging (raw JSON) → Validation → Final (production data)
                                           ↓
                                    Audit (tracking)
```

**Key Features**:
- Multi-source data integration (CRM, ERP, Accounting APIs)
- Asynchronous processing with SQS
- Data quality validation and transformation
- Comprehensive audit trail
- Real-time monitoring dashboard

## 🚀 Quick Start

### Prerequisites

- Java 17+ JDK
- Node.js 18+
- Docker Desktop
- Maven 3.8+

### 1. Start Infrastructure

```bash
# Start SQL Server and LocalStack
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 2. Run Backend (coming soon)

```bash
cd backend
./mvnw spring-boot:run
```

Backend will be available at http://localhost:8080

### 3. Run Frontend (coming soon)

```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at http://localhost:3000

## 📁 Project Structure

```
data-integration-platform/
├── backend/              # Spring Boot application
│   ├── src/main/java/   # Java source code
│   └── src/main/resources/  # Configuration and DB migrations
├── frontend/            # React/Next.js application
│   └── src/            # TypeScript source code
├── mock-apis/          # Simulated external systems
│   └── crm-api/       # Mock CRM API
├── database/           # SQL scripts and migrations
│   └── init-scripts/  # Database initialization
├── docker-compose.yml  # Infrastructure setup
├── CLAUDE.md          # Development guide and conventions
└── PROJECT_SPEC.md    # Complete technical specification
```

## 📚 Documentation

- **[CLAUDE.md](./CLAUDE.md)** - Development conventions, architecture decisions, troubleshooting
- **[PROJECT_SPEC.md](./PROJECT_SPEC.md)** - Complete technical specification with implementation phases

## 🛠️ Technology Stack

### Backend Technologies
- Spring Boot 3.x - Application framework
- Spring Data JPA - ORM and data access
- Spring Security - Authentication and authorization
- Flyway - Database migrations
- AWS SDK - Cloud service integration
- Lombok - Reduce boilerplate code

### Frontend Technologies
- React 18 - UI library
- TypeScript - Type-safe development
- Next.js 14 - React framework
- TailwindCSS - Utility-first styling
- React Query - Data fetching and caching
- Recharts - Data visualization

### Infrastructure
- SQL Server 2019 - Enterprise database
- LocalStack - AWS service mocking
- Docker - Containerization
- Docker Compose - Multi-container orchestration

## 🧪 Testing

```bash
# Backend tests
cd backend
./mvnw test

# Frontend tests
cd frontend
npm test

# E2E tests (coming soon)
npm run test:e2e
```

## 📈 Implementation Status

- [x] Project initialization and structure
- [x] Infrastructure setup (Docker Compose)
- [x] Documentation and conventions
- [ ] Spring Boot backend with dependencies
- [ ] Database schema and migrations
- [ ] Mock external APIs
- [ ] Integration services
- [ ] Data transformation pipeline
- [ ] AWS SQS integration
- [ ] React dashboard
- [ ] Comprehensive testing

## 🎓 Learning Objectives

This project demonstrates:

1. **Enterprise Architecture** - Multi-layer architecture with clear separation of concerns
2. **Data Integration Patterns** - ETL pipelines, staging, validation, and loading
3. **Cloud Integration** - AWS services (SQS, S3) with LocalStack for local development
4. **Database Design** - Complex schema design, migrations, stored procedures
5. **Modern Frontend** - React with TypeScript, state management, real-time updates
6. **Testing** - Unit, integration, and E2E testing strategies
7. **DevOps** - Docker containerization, infrastructure as code

## 📝 License

This is a portfolio project for educational and demonstration purposes.

## 🤝 Contributing

This is a personal portfolio project, but feedback and suggestions are welcome!

---

**Built with ❤️ to showcase full-stack development skills**
