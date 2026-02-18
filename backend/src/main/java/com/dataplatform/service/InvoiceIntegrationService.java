package com.dataplatform.service;

import com.dataplatform.dto.AccountingInvoiceResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.integration.AccountingApiClient;
import com.dataplatform.model.RawInvoice;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawInvoiceRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceIntegrationService {

    private final AccountingApiClient accountingApiClient;
    private final SyncJobService syncJobService;
    private final RawInvoiceRepository rawInvoiceRepository;
    private final SyncErrorRepository syncErrorRepository;
    private final ObjectMapper objectMapper;

    public SyncJobDTO syncInvoices() {
        SyncJob job = syncJobService.createJob("ACCOUNTING", "FULL");
        return syncInvoicesForJob(job);
    }

    public SyncJobDTO syncInvoicesForJob(SyncJob job) {
        List<AccountingInvoiceResponse> invoices;
        try {
            invoices = accountingApiClient.fetchAllInvoices();
        } catch (Exception ex) {
            log.error("Failed to fetch invoices from Accounting API", ex);
            syncJobService.failJob(job, "Accounting API fetch failed: " + ex.getMessage());
            return SyncJobDTO.fromEntity(job);
        }

        int processed = 0;
        int failed = 0;

        for (AccountingInvoiceResponse invoice : invoices) {
            try {
                String rawJson = objectMapper.writeValueAsString(invoice);
                RawInvoice rawInvoice = RawInvoice.builder()
                        .syncJob(job)
                        .externalId(invoice.getId())
                        .rawData(rawJson)
                        .build();
                rawInvoiceRepository.save(rawInvoice);
                processed++;
            } catch (Exception ex) {
                failed++;
                log.warn("Failed to stage invoice {}: {}", invoice.getId(), ex.getMessage());
                try {
                    SyncError error = SyncError.builder()
                            .syncJob(job)
                            .errorType("STAGING_ERROR")
                            .errorMessage(ex.getMessage())
                            .failedRecord(invoice.getId())
                            .build();
                    syncErrorRepository.save(error);
                } catch (Exception errorEx) {
                    log.error("Failed to log sync error for invoice {}", invoice.getId(), errorEx);
                }
            }
        }

        log.info("Invoice staging completed for job {}: processed={}, failed={}", job.getId(), processed, failed);
        return SyncJobDTO.fromEntity(job);
    }
}
