package com.dataplatform.integration;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.SyncErrorDTO;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.model.SyncJob;
import com.dataplatform.service.CustomerPipelineService;
import com.dataplatform.service.SyncJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SyncJobService syncJobService;

    @Autowired
    private CustomerPipelineService customerPipelineService;

    @Test
    void jobLifecycle_createToCompleted() {
        List<CrmCustomerResponse> customers = List.of(
                WireMockStubs.createCustomer("C001", "Alice Johnson", "alice@example.com", "555-1234"),
                WireMockStubs.createCustomer("C002", "Bob Smith", "bob@example.com", "555-5678")
        );
        WireMockStubs.stubCustomers(wireMockServer, customers);

        SyncJob job = syncJobService.createJob("CRM", "FULL");
        assertThat(job.getStatus()).isEqualTo("RUNNING");
        assertThat(job.getStartTime()).isNotNull();

        SyncJobDTO result = customerPipelineService.runPipelineForJob(job.getId());
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getEndTime()).isNotNull();
        assertThat(result.getRecordsProcessed()).isEqualTo(2);
        assertThat(result.getRecordsFailed()).isEqualTo(0);
    }

    @Test
    void jobLifecycle_failedJobHasEndTimeAndErrorMessage() {
        WireMockStubs.stubCustomersFailure(wireMockServer);

        SyncJob job = syncJobService.createJob("CRM", "FULL");
        SyncJobDTO result = customerPipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getEndTime()).isNotNull();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    void recentJobs_returnsInReverseChronologicalOrder() {
        syncJobService.createJob("CRM", "FULL");
        syncJobService.createJob("ERP", "FULL");
        syncJobService.createJob("ACCOUNTING", "FULL");

        List<SyncJobDTO> recentJobs = syncJobService.getRecentJobs();

        assertThat(recentJobs).hasSize(3);
        // Most recent first
        assertThat(recentJobs.get(0).getSourceName()).isEqualTo("ACCOUNTING");
        assertThat(recentJobs.get(1).getSourceName()).isEqualTo("ERP");
        assertThat(recentJobs.get(2).getSourceName()).isEqualTo("CRM");
    }

    @Test
    void errorsForJob_returnsAllErrors() {
        List<CrmCustomerResponse> customers = List.of(
                WireMockStubs.createCustomer("C001", null, "noname1@example.com", "555-1234"),  // invalid
                WireMockStubs.createCustomer("C002", null, "noname2@example.com", "555-5678"),  // invalid
                WireMockStubs.createCustomer("C003", "Valid Name", "valid@example.com", "555-9012")
        );
        WireMockStubs.stubCustomers(wireMockServer, customers);

        SyncJob job = syncJobService.createJob("CRM", "FULL");
        customerPipelineService.runPipelineForJob(job.getId());

        List<SyncErrorDTO> errors = syncJobService.getErrorsForJob(job.getId());
        assertThat(errors).hasSize(2);
        assertThat(errors).allMatch(e -> "VALIDATION_ERROR".equals(e.getErrorType()));
    }
}
