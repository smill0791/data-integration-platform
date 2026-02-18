package com.dataplatform.service;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.exception.IntegrationException;
import com.dataplatform.integration.CrmApiClient;
import com.dataplatform.model.RawCustomer;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawCustomerRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerIntegrationServiceTest {

    @Mock
    private CrmApiClient crmApiClient;

    @Mock
    private SyncJobService syncJobService;

    @Mock
    private RawCustomerRepository rawCustomerRepository;

    @Mock
    private SyncErrorRepository syncErrorRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CustomerIntegrationService customerIntegrationService;

    private SyncJob runningJob;

    @BeforeEach
    void setUp() {
        runningJob = SyncJob.builder()
                .id(1L)
                .sourceName("CRM")
                .syncType("FULL")
                .status("RUNNING")
                .startTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .recordsProcessed(0)
                .recordsFailed(0)
                .build();
    }

    private void stubCreateJob() {
        when(syncJobService.createJob("CRM", "FULL")).thenReturn(runningJob);
    }

    @Test
    void syncCustomers_success_shouldStageAllRecords() {
        stubCreateJob();
        List<CrmCustomerResponse> customers = List.of(
                buildCustomer("CRM-001", "Alice"),
                buildCustomer("CRM-002", "Bob")
        );
        when(crmApiClient.fetchAllCustomers()).thenReturn(customers);
        when(rawCustomerRepository.save(any(RawCustomer.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SyncJobDTO result = customerIntegrationService.syncCustomers();

        verify(rawCustomerRepository, times(2)).save(any(RawCustomer.class));
        verify(syncErrorRepository, never()).save(any());
    }

    @Test
    void syncCustomers_partialFailure_shouldLogErrors() {
        stubCreateJob();
        List<CrmCustomerResponse> customers = List.of(
                buildCustomer("CRM-001", "Alice"),
                buildCustomer("CRM-002", "Bob")
        );
        when(crmApiClient.fetchAllCustomers()).thenReturn(customers);
        when(rawCustomerRepository.save(any(RawCustomer.class)))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new RuntimeException("DB constraint violation"));
        when(syncErrorRepository.save(any(SyncError.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SyncJobDTO result = customerIntegrationService.syncCustomers();

        verify(syncErrorRepository).save(any(SyncError.class));
    }

    @Test
    void syncCustomers_apiFails_shouldFailJob() {
        stubCreateJob();
        when(crmApiClient.fetchAllCustomers())
                .thenThrow(new IntegrationException("Connection refused"));
        when(syncJobService.failJob(any(), anyString())).thenAnswer(inv -> {
            runningJob.setStatus("FAILED");
            runningJob.setErrorMessage(inv.getArgument(1));
            return runningJob;
        });

        SyncJobDTO result = customerIntegrationService.syncCustomers();

        verify(syncJobService).failJob(eq(runningJob), contains("Connection refused"));
        verify(rawCustomerRepository, never()).save(any());
    }

    @Test
    void syncCustomers_emptyResponse_shouldCompleteWithZero() {
        stubCreateJob();
        when(crmApiClient.fetchAllCustomers()).thenReturn(Collections.emptyList());

        SyncJobDTO result = customerIntegrationService.syncCustomers();

        verify(rawCustomerRepository, never()).save(any());
    }

    @Test
    void syncCustomersForJob_shouldUseExistingJob() {
        List<CrmCustomerResponse> customers = List.of(
                buildCustomer("CRM-001", "Alice")
        );
        when(crmApiClient.fetchAllCustomers()).thenReturn(customers);
        when(rawCustomerRepository.save(any(RawCustomer.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SyncJobDTO result = customerIntegrationService.syncCustomersForJob(runningJob);

        verify(syncJobService, never()).createJob(any(), any());
        verify(rawCustomerRepository).save(any(RawCustomer.class));
    }

    private CrmCustomerResponse buildCustomer(String id, String name) {
        return CrmCustomerResponse.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase() + "@example.com")
                .phone("555-0100")
                .address(CrmCustomerResponse.Address.builder()
                        .street("123 Main St")
                        .city("Springfield")
                        .state("IL")
                        .zipCode("62701")
                        .build())
                .lastUpdated("2024-01-15T10:30:00Z")
                .build();
    }
}
