package com.dataplatform.integration;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.*;
import com.dataplatform.service.CustomerPipelineService;
import com.dataplatform.service.SyncJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerPipelineIntegrationTest extends BaseIntegrationTest {

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
    void fullCustomerPipeline_shouldPopulateAllSchemas() {
        List<CrmCustomerResponse> customers = List.of(
                WireMockStubs.createCustomer("C001", "Alice Johnson", "alice@example.com", "555-1234"),
                WireMockStubs.createCustomer("C002", "Bob Smith", "bob@example.com", "555-5678"),
                WireMockStubs.createCustomer("C003", "Carol White", "carol@example.com", "555-9012")
        );
        WireMockStubs.stubCustomers(wireMockServer, customers);

        SyncJob job = syncJobService.createJob("CRM", "FULL");
        SyncJobDTO result = customerPipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(rawCustomerRepository.findBySyncJobId(job.getId())).hasSize(3);
        assertThat(validatedCustomerRepository.findAll()).hasSize(3);
        assertThat(finalCustomerRepository.findAll()).hasSize(3);
        assertThat(syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(job.getId())).isEmpty();
    }

    @Test
    void customerUpsert_secondSyncUpdatesExistingRecords() {
        // First sync
        List<CrmCustomerResponse> customers1 = List.of(
                WireMockStubs.createCustomer("C001", "Alice Johnson", "alice@example.com", "555-1234"),
                WireMockStubs.createCustomer("C002", "Bob Smith", "bob@example.com", "555-5678")
        );
        WireMockStubs.stubCustomers(wireMockServer, customers1);

        SyncJob job1 = syncJobService.createJob("CRM", "FULL");
        customerPipelineService.runPipelineForJob(job1.getId());

        var firstSync = finalCustomerRepository.findByExternalId("C001");
        assertThat(firstSync).isPresent();
        var firstSyncedAt = firstSync.get().getLastSyncedAt();

        // Second sync with updated names
        wireMockServer.resetAll();
        List<CrmCustomerResponse> customers2 = List.of(
                WireMockStubs.createCustomer("C001", "Alice Updated", "alice@example.com", "555-1234"),
                WireMockStubs.createCustomer("C002", "Bob Updated", "bob@example.com", "555-5678")
        );
        WireMockStubs.stubCustomers(wireMockServer, customers2);

        SyncJob job2 = syncJobService.createJob("CRM", "FULL");
        customerPipelineService.runPipelineForJob(job2.getId());

        // MERGE should update, not duplicate
        assertThat(finalCustomerRepository.findAll()).hasSize(2);

        var updated = finalCustomerRepository.findByExternalId("C001");
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("Alice Updated");
        assertThat(updated.get().getLastSyncedAt()).isAfterOrEqualTo(firstSyncedAt);
    }

    @Test
    void customerPipeline_partialValidationFailure() {
        List<CrmCustomerResponse> customers = List.of(
                WireMockStubs.createCustomer("C001", "Alice Johnson", "alice@example.com", "555-1234"),
                WireMockStubs.createCustomer("C002", null, "noname@example.com", "555-5678"),  // null name
                WireMockStubs.createCustomer("C003", "Carol White", "carol@example.com", "555-9012")
        );
        WireMockStubs.stubCustomers(wireMockServer, customers);

        SyncJob job = syncJobService.createJob("CRM", "FULL");
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
    void customerPipeline_apiFailure_shouldMarkJobFailed() {
        WireMockStubs.stubCustomersFailure(wireMockServer);

        SyncJob job = syncJobService.createJob("CRM", "FULL");
        SyncJobDTO result = customerPipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(rawCustomerRepository.findBySyncJobId(job.getId())).isEmpty();
        assertThat(finalCustomerRepository.findAll()).isEmpty();
    }

    @Test
    void customerTransformation_normalizesPhoneAndEmail() {
        List<CrmCustomerResponse> customers = List.of(
                WireMockStubs.createCustomer("C001", "Alice Johnson", "ALICE@Example.COM", "(555) 123-4567")
        );
        WireMockStubs.stubCustomers(wireMockServer, customers);

        SyncJob job = syncJobService.createJob("CRM", "FULL");
        customerPipelineService.runPipelineForJob(job.getId());

        var validated = validatedCustomerRepository.findByExternalId("C001");
        assertThat(validated).isPresent();
        assertThat(validated.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(validated.get().getPhone()).isEqualTo("5551234567");

        var finalCust = finalCustomerRepository.findByExternalId("C001");
        assertThat(finalCust).isPresent();
        assertThat(finalCust.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(finalCust.get().getPhone()).isEqualTo("5551234567");
    }
}
