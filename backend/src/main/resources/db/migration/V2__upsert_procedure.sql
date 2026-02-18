-- Stored procedure for upserting customers into final schema
CREATE PROCEDURE [final].upsert_customers
    @external_id VARCHAR(100),
    @name VARCHAR(200),
    @email VARCHAR(200),
    @phone VARCHAR(50),
    @address VARCHAR(500),
    @source_system VARCHAR(100)
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
            source_system = @source_system,
            last_synced_at = GETDATE(),
            updated_at = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (external_id, name, email, phone, address, source_system, first_synced_at, last_synced_at, is_active, created_at, updated_at)
        VALUES (@external_id, @name, @email, @phone, @address, @source_system, GETDATE(), GETDATE(), 1, GETDATE(), GETDATE());
END;
GO
