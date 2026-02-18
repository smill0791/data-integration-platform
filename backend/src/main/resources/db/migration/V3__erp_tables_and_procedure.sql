-- Staging: Raw product data from ERP
CREATE TABLE staging.raw_products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sync_job_id BIGINT NOT NULL,
    external_id VARCHAR(100),
    raw_data VARCHAR(MAX),
    received_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (sync_job_id) REFERENCES audit.sync_jobs(id)
);
GO

-- Validated: Products that passed validation
CREATE TABLE validated.validated_products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(200),
    unit_price DECIMAL(18,2),
    quantity INT,
    warehouse VARCHAR(200),
    validated_at DATETIME2 DEFAULT GETDATE()
);
GO

-- Final: Production product data
CREATE TABLE [final].products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(200),
    unit_price DECIMAL(18,2),
    quantity INT,
    warehouse VARCHAR(200),
    source_system VARCHAR(100),
    first_synced_at DATETIME2 NOT NULL,
    last_synced_at DATETIME2 NOT NULL,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_final_products_external_id ON [final].products(external_id);
CREATE INDEX idx_final_products_sku ON [final].products(sku);
GO

-- Stored procedure for upserting products into final schema
CREATE PROCEDURE [final].upsert_products
    @external_id VARCHAR(100),
    @sku VARCHAR(100),
    @name VARCHAR(200),
    @description VARCHAR(1000),
    @category VARCHAR(200),
    @unit_price DECIMAL(18,2),
    @quantity INT,
    @warehouse VARCHAR(200),
    @source_system VARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;

    MERGE [final].products AS target
    USING (SELECT @external_id AS external_id) AS source
    ON target.external_id = source.external_id
    WHEN MATCHED THEN
        UPDATE SET
            sku = @sku,
            name = @name,
            description = @description,
            category = @category,
            unit_price = @unit_price,
            quantity = @quantity,
            warehouse = @warehouse,
            source_system = @source_system,
            last_synced_at = GETDATE(),
            updated_at = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (external_id, sku, name, description, category, unit_price, quantity, warehouse, source_system, first_synced_at, last_synced_at, is_active, created_at, updated_at)
        VALUES (@external_id, @sku, @name, @description, @category, @unit_price, @quantity, @warehouse, @source_system, GETDATE(), GETDATE(), 1, GETDATE(), GETDATE());
END;
GO
