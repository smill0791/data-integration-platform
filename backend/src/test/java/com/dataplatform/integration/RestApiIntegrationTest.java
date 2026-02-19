package com.dataplatform.integration;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.SyncErrorDTO;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.service.SyncJobService;
import com.dataplatform.service.SyncMessageProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

class RestApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SyncJobService syncJobService;

    @Autowired
    private SyncErrorRepository syncErrorRepository;

    @MockBean
    private SyncMessageProducer syncMessageProducer;

    @Test
    void getRecentJobs_returnsJobList() {
        syncJobService.createJob("CRM", "FULL");
        syncJobService.createJob("ERP", "FULL");

        ResponseEntity<List<SyncJobDTO>> response = restTemplate.exchange(
                "/api/integrations/jobs",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getJobById_returnsJobDetails() {
        SyncJob job = syncJobService.createJob("CRM", "FULL");

        ResponseEntity<SyncJobDTO> response = restTemplate.getForEntity(
                "/api/integrations/jobs/" + job.getId(),
                SyncJobDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSourceName()).isEqualTo("CRM");
        assertThat(response.getBody().getStatus()).isEqualTo("RUNNING");
    }

    @Test
    void getJobById_notFound_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/integrations/jobs/99999",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getJobErrors_returnsErrors() {
        SyncJob job = syncJobService.createJob("CRM", "FULL");
        SyncError error = SyncError.builder()
                .syncJob(job)
                .errorType("VALIDATION_ERROR")
                .errorMessage("Missing required field: name")
                .failedRecord("C001")
                .build();
        syncErrorRepository.save(error);

        ResponseEntity<List<SyncErrorDTO>> response = restTemplate.exchange(
                "/api/integrations/jobs/" + job.getId() + "/errors",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getErrorType()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void syncCustomers_returns202Accepted() {
        ResponseEntity<SyncJobDTO> response = restTemplate.postForEntity(
                "/api/integrations/sync/customers",
                null,
                SyncJobDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("QUEUED");
        assertThat(response.getBody().getSourceName()).isEqualTo("CRM");

        verify(syncMessageProducer).sendSyncRequest(
                eq(response.getBody().getId()),
                eq("CRM"),
                eq("FULL")
        );
    }
}
