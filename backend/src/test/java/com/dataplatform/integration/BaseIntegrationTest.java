package com.dataplatform.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Testcontainers
public abstract class BaseIntegrationTest {

    static final MSSQLServerContainer<?> sqlServer;
    static final WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 18089;

    static {
        sqlServer = new MSSQLServerContainer<>(
                DockerImageName.parse("mcr.microsoft.com/azure-sql-edge:latest")
                        .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
                .withPassword("YourStrong@Passw0rd")
                .acceptLicense()
                .withInitScript("integration-test-init.sql");
        sqlServer.start();

        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(WIREMOCK_PORT));
        wireMockServer.start();
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", sqlServer::getJdbcUrl);
        registry.add("spring.datasource.username", sqlServer::getUsername);
        registry.add("spring.datasource.password", sqlServer::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM audit.sync_errors");
        jdbcTemplate.execute("DELETE FROM staging.raw_customers");
        jdbcTemplate.execute("DELETE FROM staging.raw_products");
        jdbcTemplate.execute("DELETE FROM staging.raw_invoices");
        jdbcTemplate.execute("DELETE FROM validated.validated_customers");
        jdbcTemplate.execute("DELETE FROM validated.validated_products");
        jdbcTemplate.execute("DELETE FROM validated.validated_invoices");
        jdbcTemplate.execute("DELETE FROM [final].customers");
        jdbcTemplate.execute("DELETE FROM [final].products");
        jdbcTemplate.execute("DELETE FROM [final].invoices");
        jdbcTemplate.execute("DELETE FROM audit.sync_jobs");

        wireMockServer.resetAll();
    }
}
