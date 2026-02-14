-- Create schemas
CREATE SCHEMA audit;
GO

CREATE SCHEMA staging;
GO

CREATE SCHEMA validated;
GO

CREATE SCHEMA [final];
GO

-- Audit: Sync job tracking
CREATE TABLE audit.sync_jobs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    source_name VARCHAR(100) NOT NULL,
    sync_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_time DATETIME2 NOT NULL,
    end_time DATETIME2,
    records_processed INT DEFAULT 0,
    records_failed INT DEFAULT 0,
    error_message VARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_sync_jobs_status ON audit.sync_jobs(status);
CREATE INDEX idx_sync_jobs_source_start ON audit.sync_jobs(source_name, start_time);
GO

-- Audit: Error logging
CREATE TABLE audit.sync_errors (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sync_job_id BIGINT NOT NULL,
    error_type VARCHAR(100),
    error_message VARCHAR(MAX),
    failed_record VARCHAR(MAX),
    occurred_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id)
);
GO

CREATE INDEX idx_sync_errors_job ON audit.sync_errors(sync_job_id);
GO

-- Staging: Raw customer data
CREATE TABLE staging.raw_customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sync_job_id BIGINT NOT NULL,
    external_id VARCHAR(100),
    raw_data VARCHAR(MAX),
    received_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id)
);
GO

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
GO

-- Final: Production customer data
CREATE TABLE [final].customers (
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
    updated_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_final_customers_external_id ON [final].customers(external_id);
CREATE INDEX idx_final_customers_email ON [final].customers(email);
GO
