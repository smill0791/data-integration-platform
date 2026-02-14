IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'dataintegration')
BEGIN
    CREATE DATABASE dataintegration;
END
GO
