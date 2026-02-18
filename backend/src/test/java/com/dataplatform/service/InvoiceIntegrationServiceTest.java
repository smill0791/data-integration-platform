package com.dataplatform.service;

import com.dataplatform.dto.AccountingInvoiceResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.exception.IntegrationException;
import com.dataplatform.integration.AccountingApiClient;
import com.dataplatform.model.RawInvoice;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawInvoiceRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceIntegrationServiceTest {

    @Mock private AccountingApiClient accountingApiClient;
    @Mock private SyncJobService syncJobService;
    @Mock private RawInvoiceRepository rawInvoiceRepository;
    @Mock private SyncErrorRepository syncErrorRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InvoiceIntegrationService invoiceIntegrationService;

    private SyncJob runningJob;

    @BeforeEach
    void setUp() {
        runningJob = SyncJob.builder()
                .id(1L).sourceName("ACCOUNTING").syncType("FULL").status("RUNNING")
                .startTime(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .recordsProcessed(0).recordsFailed(0).build();
    }

    @Test
    void syncInvoices_success_shouldStageAllRecords() {
        when(syncJobService.createJob("ACCOUNTING", "FULL")).thenReturn(runningJob);
        List<AccountingInvoiceResponse> invoices = List.of(
                buildInvoice("ACC-001", "INV-001"),
                buildInvoice("ACC-002", "INV-002")
        );
        when(accountingApiClient.fetchAllInvoices()).thenReturn(invoices);
        when(rawInvoiceRepository.save(any(RawInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceIntegrationService.syncInvoices();

        verify(rawInvoiceRepository, times(2)).save(any(RawInvoice.class));
        verify(syncErrorRepository, never()).save(any());
    }

    @Test
    void syncInvoices_partialFailure_shouldLogErrors() {
        when(syncJobService.createJob("ACCOUNTING", "FULL")).thenReturn(runningJob);
        List<AccountingInvoiceResponse> invoices = List.of(
                buildInvoice("ACC-001", "INV-001"),
                buildInvoice("ACC-002", "INV-002")
        );
        when(accountingApiClient.fetchAllInvoices()).thenReturn(invoices);
        when(rawInvoiceRepository.save(any(RawInvoice.class)))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new RuntimeException("DB constraint violation"));
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceIntegrationService.syncInvoices();

        verify(syncErrorRepository).save(any(SyncError.class));
    }

    @Test
    void syncInvoices_apiFails_shouldFailJob() {
        when(syncJobService.createJob("ACCOUNTING", "FULL")).thenReturn(runningJob);
        when(accountingApiClient.fetchAllInvoices()).thenThrow(new IntegrationException("Connection refused"));
        when(syncJobService.failJob(any(), anyString())).thenAnswer(inv -> {
            runningJob.setStatus("FAILED");
            return runningJob;
        });

        SyncJobDTO result = invoiceIntegrationService.syncInvoices();

        verify(syncJobService).failJob(eq(runningJob), contains("Connection refused"));
        verify(rawInvoiceRepository, never()).save(any());
    }

    @Test
    void syncInvoices_emptyResponse_shouldCompleteWithZero() {
        when(syncJobService.createJob("ACCOUNTING", "FULL")).thenReturn(runningJob);
        when(accountingApiClient.fetchAllInvoices()).thenReturn(Collections.emptyList());

        invoiceIntegrationService.syncInvoices();

        verify(rawInvoiceRepository, never()).save(any());
    }

    @Test
    void syncInvoicesForJob_shouldUseExistingJob() {
        List<AccountingInvoiceResponse> invoices = List.of(buildInvoice("ACC-001", "INV-001"));
        when(accountingApiClient.fetchAllInvoices()).thenReturn(invoices);
        when(rawInvoiceRepository.save(any(RawInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceIntegrationService.syncInvoicesForJob(runningJob);

        verify(syncJobService, never()).createJob(any(), any());
        verify(rawInvoiceRepository).save(any(RawInvoice.class));
    }

    private AccountingInvoiceResponse buildInvoice(String id, String invoiceNumber) {
        return AccountingInvoiceResponse.builder()
                .id(id).invoiceNumber(invoiceNumber).customerName("Acme Corp")
                .amount(1500.50).currency("USD").status("paid").dueDate("2025-06-15")
                .lineItems(List.of(Map.of("description", "Item 1", "quantity", 1, "unitPrice", 1500.50)))
                .build();
    }
}
