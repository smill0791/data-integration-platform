-- Staging: Raw invoice data from Accounting
CREATE TABLE staging.raw_invoices (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sync_job_id BIGINT NOT NULL,
    external_id VARCHAR(100),
    raw_data VARCHAR(MAX),
    received_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id)
);
GO

-- Validated: Invoices that passed validation
CREATE TABLE validated.validated_invoices (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    invoice_number VARCHAR(100) NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    amount DECIMAL(18,2),
    currency VARCHAR(10),
    status VARCHAR(50),
    due_date DATE,
    validated_at DATETIME2 DEFAULT GETDATE()
);
GO

-- Final: Production invoice data
CREATE TABLE [final].invoices (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    invoice_number VARCHAR(100) NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    amount DECIMAL(18,2),
    currency VARCHAR(10),
    status VARCHAR(50),
    due_date DATE,
    source_system VARCHAR(100),
    first_synced_at DATETIME2 NOT NULL,
    last_synced_at DATETIME2 NOT NULL,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_final_invoices_external_id ON [final].invoices(external_id);
CREATE INDEX idx_final_invoices_invoice_number ON [final].invoices(invoice_number);
GO

-- Stored procedure for upserting invoices into final schema
CREATE PROCEDURE [final].upsert_invoices
    @external_id VARCHAR(100),
    @invoice_number VARCHAR(100),
    @customer_name VARCHAR(200),
    @amount DECIMAL(18,2),
    @currency VARCHAR(10),
    @status VARCHAR(50),
    @due_date DATE,
    @source_system VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    MERGE [final].invoices AS target
    USING (SELECT @external_id AS external_id) AS source
    ON target.external_id = source.external_id
    WHEN MATCHED THEN
        UPDATE SET
            invoice_number = @invoice_number,
            customer_name = @customer_name,
            amount = @amount,
            currency = @currency,
            status = @status,
            due_date = @due_date,
            source_system = @source_system,
            last_synced_at = GETDATE(),
            updated_at = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (external_id, invoice_number, customer_name, amount, currency, status, due_date, source_system, first_synced_at, last_synced_at, is_active, created_at, updated_at)
        VALUES (@external_id, @invoice_number, @customer_name, @amount, @currency, @status, @due_date, @source_system, GETDATE(), GETDATE(), 1, GETDATE(), GETDATE());
END;
GO
