package com.dataplatform.service;

import com.dataplatform.dto.SalesforceContact;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.exception.IntegrationException;
import com.dataplatform.integration.SalesforceApiClient;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesforceIntegrationServiceTest {

    @Mock private SalesforceApiClient salesforceApiClient;
    @Mock private RawCustomerRepository rawCustomerRepository;
    @Mock private SyncErrorRepository syncErrorRepository;
    @Mock private SyncJobService syncJobService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SalesforceIntegrationService service;

    private SyncJob job;

    @BeforeEach
    void setUp() {
        job = SyncJob.builder()
                .id(1L)
                .sourceName("SALESFORCE")
                .syncType("FULL")
                .status("RUNNING")
                .startTime(LocalDateTime.now())
                .build();
    }

    @Test
    void syncContactsForJob_success_shouldStageAllContacts() {
        List<SalesforceContact> contacts = List.of(
                SalesforceContact.builder().id("003A").firstName("Alice").lastName("Smith")
                        .email("alice@sf.com").phone("555-1234").build(),
                SalesforceContact.builder().id("003B").firstName("Bob").lastName("Jones")
                        .email("bob@sf.com").phone("555-5678").build()
        );
        when(salesforceApiClient.fetchContacts()).thenReturn(contacts);
        when(rawCustomerRepository.save(any(RawCustomer.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncJobDTO result = service.syncContactsForJob(job);

        assertThat(result.getStatus()).isEqualTo("RUNNING");
        verify(rawCustomerRepository, times(2)).save(argThat(rc ->
                rc.getExternalId() != null && rc.getRawData().contains("name")));
    }

    @Test
    void syncContactsForJob_normalizesContactToCrmFormat() {
        SalesforceContact contact = SalesforceContact.builder()
                .id("003C").firstName("Carol").lastName("White")
                .email("carol@sf.com").phone("555-9999")
                .mailingStreet("100 Main St").mailingCity("Denver")
                .mailingState("CO").mailingPostalCode("80201")
                .lastModifiedDate("2024-01-15T10:30:00.000+0000")
                .build();
        when(salesforceApiClient.fetchContacts()).thenReturn(List.of(contact));
        when(rawCustomerRepository.save(any(RawCustomer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.syncContactsForJob(job);

        verify(rawCustomerRepository).save(argThat(rc -> {
            String json = rc.getRawData();
            return json.contains("Carol White")
                    && json.contains("carol@sf.com")
                    && json.contains("100 Main St")
                    && json.contains("Denver")
                    && "003C".equals(rc.getExternalId());
        }));
    }

    @Test
    void syncContactsForJob_apiFailure_shouldFailJob() {
        when(salesforceApiClient.fetchContacts()).thenThrow(new IntegrationException("Auth failed"));
        when(syncJobService.failJob(any(), any())).thenAnswer(inv -> {
            job.setStatus("FAILED");
            return job;
        });

        SyncJobDTO result = service.syncContactsForJob(job);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(syncJobService).failJob(eq(job), contains("Salesforce API fetch failed"));
    }

    @Test
    void syncContactsForJob_perRecordErrorIsolation() {
        SalesforceContact good = SalesforceContact.builder().id("003A").firstName("Alice").lastName("Smith").build();
        SalesforceContact bad = SalesforceContact.builder().id("003B").firstName("Bad").lastName("Record").build();
        when(salesforceApiClient.fetchContacts()).thenReturn(List.of(good, bad));
        when(rawCustomerRepository.save(any(RawCustomer.class)))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new RuntimeException("DB error"));

        service.syncContactsForJob(job);

        verify(rawCustomerRepository, times(2)).save(any(RawCustomer.class));
        verify(syncErrorRepository).save(argThat(err ->
                "STAGING_ERROR".equals(err.getErrorType()) && "003B".equals(err.getFailedRecord())));
    }

    @Test
    void syncContactsForJob_emptyResponse() {
        when(salesforceApiClient.fetchContacts()).thenReturn(List.of());

        SyncJobDTO result = service.syncContactsForJob(job);

        assertThat(result.getStatus()).isEqualTo("RUNNING");
        verify(rawCustomerRepository, never()).save(any());
    }
}
