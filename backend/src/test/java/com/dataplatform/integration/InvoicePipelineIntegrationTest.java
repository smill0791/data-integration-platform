package com.dataplatform.integration;

import com.dataplatform.dto.AccountingInvoiceResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.*;
import com.dataplatform.service.InvoicePipelineService;
import com.dataplatform.service.SyncJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePipelineIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private InvoicePipelineService invoicePipelineService;

    @Autowired
    private SyncJobService syncJobService;

    @Autowired
    private RawInvoiceRepository rawInvoiceRepository;

    @Autowired
    private ValidatedInvoiceRepository validatedInvoiceRepository;

    @Autowired
    private FinalInvoiceRepository finalInvoiceRepository;

    @Autowired
    private SyncErrorRepository syncErrorRepository;

    @Test
    void fullInvoicePipeline_shouldPopulateAllSchemas() {
        List<AccountingInvoiceResponse> invoices = List.of(
                WireMockStubs.createInvoice("I001", "INV-001", "Acme Corp", 1500.00, "usd", "Paid"),
                WireMockStubs.createInvoice("I002", "INV-002", "Globex Inc", 2500.00, "eur", "Pending"),
                WireMockStubs.createInvoice("I003", "INV-003", "Initech LLC", 750.00, "usd", "Overdue")
        );
        WireMockStubs.stubInvoices(wireMockServer, invoices);

        SyncJob job = syncJobService.createJob("ACCOUNTING", "FULL");
        SyncJobDTO result = invoicePipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(rawInvoiceRepository.findBySyncJobId(job.getId())).hasSize(3);
        assertThat(validatedInvoiceRepository.findAll()).hasSize(3);
        assertThat(finalInvoiceRepository.findAll()).hasSize(3);

        // Currency should be uppercased, status lowercased
        var finalInvoice = finalInvoiceRepository.findByExternalId("I001");
        assertThat(finalInvoice).isPresent();
        assertThat(finalInvoice.get().getCurrency()).isEqualTo("USD");
        assertThat(finalInvoice.get().getStatus()).isEqualTo("paid");
    }

    @Test
    void invoiceUpsert_secondSyncUpdates() {
        List<AccountingInvoiceResponse> invoices1 = List.of(
                WireMockStubs.createInvoice("I001", "INV-001", "Acme Corp", 1500.00, "usd", "Pending"),
                WireMockStubs.createInvoice("I002", "INV-002", "Globex Inc", 2500.00, "eur", "Pending")
        );
        WireMockStubs.stubInvoices(wireMockServer, invoices1);

        SyncJob job1 = syncJobService.createJob("ACCOUNTING", "FULL");
        invoicePipelineService.runPipelineForJob(job1.getId());

        // Second sync with updated status
        wireMockServer.resetAll();
        List<AccountingInvoiceResponse> invoices2 = List.of(
                WireMockStubs.createInvoice("I001", "INV-001", "Acme Corp", 1500.00, "usd", "Paid"),
                WireMockStubs.createInvoice("I002", "INV-002", "Globex Inc", 2500.00, "eur", "Paid")
        );
        WireMockStubs.stubInvoices(wireMockServer, invoices2);

        SyncJob job2 = syncJobService.createJob("ACCOUNTING", "FULL");
        invoicePipelineService.runPipelineForJob(job2.getId());

        // MERGE should update, not duplicate
        assertThat(finalInvoiceRepository.findAll()).hasSize(2);

        var updated = finalInvoiceRepository.findByExternalId("I001");
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo("paid");
    }

    @Test
    void invoicePipeline_invalidStatus_failsValidation() {
        List<AccountingInvoiceResponse> invoices = List.of(
                WireMockStubs.createInvoice("I001", "INV-001", "Acme Corp", 1500.00, "usd", "paid"),
                WireMockStubs.createInvoice("I002", "INV-002", "Globex Inc", 2500.00, "eur", "INVALID")  // invalid status
        );
        WireMockStubs.stubInvoices(wireMockServer, invoices);

        SyncJob job = syncJobService.createJob("ACCOUNTING", "FULL");
        SyncJobDTO result = invoicePipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRecordsProcessed()).isEqualTo(1);
        assertThat(result.getRecordsFailed()).isEqualTo(1);
        assertThat(finalInvoiceRepository.findAll()).hasSize(1);

        var errors = syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(job.getId());
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getErrorType()).isEqualTo("VALIDATION_ERROR");
    }
}
