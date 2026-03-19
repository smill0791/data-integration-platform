package com.dataplatform.integration;

import com.dataplatform.dto.SalesforceContact;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.*;
import com.dataplatform.service.CustomerPipelineService;
import com.dataplatform.service.SyncJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesforcePipelineIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CustomerPipelineService customerPipelineService;

    @Autowired
    private SyncJobService syncJobService;

    @Autowired
    private RawCustomerRepository rawCustomerRepository;

    @Autowired
    private ValidatedCustomerRepository validatedCustomerRepository;

    @Autowired
    private FinalCustomerRepository finalCustomerRepository;

    @Autowired
    private SyncErrorRepository syncErrorRepository;

    @Test
    void fullSalesforcePipeline_shouldPopulateAllSchemasWithSalesforceSource() {
        WireMockStubs.stubSalesforceAuth(wireMockServer);
        List<SalesforceContact> contacts = List.of(
                WireMockStubs.createSalesforceContact("003A", "Alice", "Johnson", "alice@sf.com", "555-1234"),
                WireMockStubs.createSalesforceContact("003B", "Bob", "Smith", "bob@sf.com", "555-5678")
        );
        WireMockStubs.stubSalesforceContacts(wireMockServer, contacts);

        SyncJob job = syncJobService.createJob("SALESFORCE", "FULL");
        SyncJobDTO result = customerPipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(rawCustomerRepository.findBySyncJobId(job.getId())).hasSize(2);
        assertThat(validatedCustomerRepository.findAll()).hasSize(2);
        assertThat(finalCustomerRepository.findAll()).hasSize(2);

        // Verify source_system is SALESFORCE in final table
        var finalCustomer = finalCustomerRepository.findByExternalId("003A");
        assertThat(finalCustomer).isPresent();
        assertThat(finalCustomer.get().getSourceSystem()).isEqualTo("SALESFORCE");
        assertThat(finalCustomer.get().getName()).isEqualTo("Alice Johnson");
    }

    @Test
    void salesforceUpsert_secondSyncUpdatesExistingRecords() {
        // First sync
        WireMockStubs.stubSalesforceAuth(wireMockServer);
        List<SalesforceContact> contacts1 = List.of(
                WireMockStubs.createSalesforceContact("003A", "Alice", "Johnson", "alice@sf.com", "555-1234")
        );
        WireMockStubs.stubSalesforceContacts(wireMockServer, contacts1);

        SyncJob job1 = syncJobService.createJob("SALESFORCE", "FULL");
        customerPipelineService.runPipelineForJob(job1.getId());

        assertThat(finalCustomerRepository.findAll()).hasSize(1);
        assertThat(finalCustomerRepository.findByExternalId("003A").get().getName()).isEqualTo("Alice Johnson");

        // Second sync with updated name
        wireMockServer.resetAll();
        WireMockStubs.stubSalesforceAuth(wireMockServer);
        List<SalesforceContact> contacts2 = List.of(
                WireMockStubs.createSalesforceContact("003A", "Alice", "Updated", "alice@sf.com", "555-1234")
        );
        WireMockStubs.stubSalesforceContacts(wireMockServer, contacts2);

        SyncJob job2 = syncJobService.createJob("SALESFORCE", "FULL");
        customerPipelineService.runPipelineForJob(job2.getId());

        // MERGE should update, not duplicate
        assertThat(finalCustomerRepository.findAll()).hasSize(1);
        assertThat(finalCustomerRepository.findByExternalId("003A").get().getName()).isEqualTo("Alice Updated");
    }

    @Test
    void salesforcePipeline_partialValidationFailure() {
        WireMockStubs.stubSalesforceAuth(wireMockServer);
        List<SalesforceContact> contacts = List.of(
                WireMockStubs.createSalesforceContact("003A", "Alice", "Johnson", "alice@sf.com", "555-1234"),
                WireMockStubs.createSalesforceContact("003B", null, null, "noname@sf.com", "555-5678"),  // null name
                WireMockStubs.createSalesforceContact("003C", "Carol", "White", "carol@sf.com", "555-9012")
        );
        WireMockStubs.stubSalesforceContacts(wireMockServer, contacts);

        SyncJob job = syncJobService.createJob("SALESFORCE", "FULL");
        SyncJobDTO result = customerPipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRecordsProcessed()).isEqualTo(2);
        assertThat(result.getRecordsFailed()).isEqualTo(1);
        assertThat(finalCustomerRepository.findAll()).hasSize(2);

        var errors = syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(job.getId());
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getErrorType()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void salesforcePipeline_normalizesContactData() {
        WireMockStubs.stubSalesforceAuth(wireMockServer);
        List<SalesforceContact> contacts = List.of(
                SalesforceContact.builder()
                        .id("003D")
                        .firstName("Dave")
                        .lastName("Wilson")
                        .email("DAVE@Example.COM")
                        .phone("(555) 123-4567")
                        .mailingStreet("789 Pine St")
                        .mailingCity("Chicago")
                        .mailingState("IL")
                        .mailingPostalCode("60601")
                        .lastModifiedDate("2024-01-15T10:30:00.000+0000")
                        .build()
        );
        WireMockStubs.stubSalesforceContacts(wireMockServer, contacts);

        SyncJob job = syncJobService.createJob("SALESFORCE", "FULL");
        customerPipelineService.runPipelineForJob(job.getId());

        var validated = validatedCustomerRepository.findByExternalId("003D");
        assertThat(validated).isPresent();
        assertThat(validated.get().getEmail()).isEqualTo("dave@example.com");
        assertThat(validated.get().getPhone()).isEqualTo("5551234567");
        assertThat(validated.get().getName()).isEqualTo("Dave Wilson");

        var finalCust = finalCustomerRepository.findByExternalId("003D");
        assertThat(finalCust).isPresent();
        assertThat(finalCust.get().getSourceSystem()).isEqualTo("SALESFORCE");
    }
}
