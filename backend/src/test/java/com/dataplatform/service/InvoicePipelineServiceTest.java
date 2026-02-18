package com.dataplatform.service;

import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.dto.TransformedInvoice;
import com.dataplatform.dto.ValidationResult;
import com.dataplatform.model.RawInvoice;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawInvoiceRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.transformer.InvoiceTransformationService;
import com.dataplatform.validator.InvoiceValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoicePipelineServiceTest {

    @Mock private InvoiceIntegrationService invoiceIntegrationService;
    @Mock private InvoiceTransformationService transformationService;
    @Mock private InvoiceValidationService validationService;
    @Mock private InvoiceLoadService loadService;
    @Mock private SyncJobService syncJobService;
    @Mock private RawInvoiceRepository rawInvoiceRepository;
    @Mock private SyncErrorRepository syncErrorRepository;

    @InjectMocks
    private InvoicePipelineService pipelineService;

    private SyncJob runningJob;
    private SyncJobDTO stagingResult;

    @BeforeEach
    void setUp() {
        runningJob = SyncJob.builder()
                .id(1L).sourceName("ACCOUNTING").syncType("FULL").status("COMPLETED")
                .startTime(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .recordsProcessed(2).recordsFailed(0).build();

        stagingResult = SyncJobDTO.builder()
                .id(1L).sourceName("ACCOUNTING").syncType("FULL").status("COMPLETED")
                .recordsProcessed(2).recordsFailed(0).build();
    }

    @Test
    void runFullPipeline_allSuccess_shouldLoadAllRecords() {
        when(invoiceIntegrationService.syncInvoices()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawInvoiceRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawInvoice("ACC-001", "{\"id\":\"ACC-001\"}"),
                buildRawInvoice("ACC-002", "{\"id\":\"ACC-002\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("ACC-001"));
        when(validationService.validate(any())).thenReturn(validResult());
        when(syncJobService.completeJob(any(), eq(2), eq(0))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(loadService, times(2)).loadInvoice(any());
        verify(syncJobService).completeJob(runningJob, 2, 0);
    }

    @Test
    void runFullPipeline_stagingFails_shouldReturnEarly() {
        SyncJobDTO failedStaging = SyncJobDTO.builder().id(1L).status("FAILED").build();
        when(invoiceIntegrationService.syncInvoices()).thenReturn(failedStaging);

        SyncJobDTO result = pipelineService.runFullPipeline();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(loadService, never()).loadInvoice(any());
    }

    @Test
    void runFullPipeline_validationFailure_shouldLogErrorAndContinue() {
        when(invoiceIntegrationService.syncInvoices()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawInvoiceRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawInvoice("ACC-001", "{\"id\":\"ACC-001\"}"),
                buildRawInvoice("ACC-002", "{\"id\":\"ACC-002\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("ACC-001"));
        ValidationResult invalid = ValidationResult.builder().valid(false).errors(List.of("customer_name is required")).build();
        when(validationService.validate(any())).thenReturn(invalid, validResult());
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(loadService, times(1)).loadInvoice(any());
        verify(syncErrorRepository).save(argThat(err -> "VALIDATION_ERROR".equals(err.getErrorType())));
    }

    @Test
    void runFullPipeline_transformFailure_shouldLogErrorAndContinue() {
        when(invoiceIntegrationService.syncInvoices()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawInvoiceRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawInvoice("ACC-001", "bad json"),
                buildRawInvoice("ACC-002", "{\"id\":\"ACC-002\"}")
        ));
        when(transformationService.transform("bad json")).thenThrow(new IllegalArgumentException("Failed to parse"));
        when(transformationService.transform("{\"id\":\"ACC-002\"}")).thenReturn(buildTransformed("ACC-002"));
        when(validationService.validate(any())).thenReturn(validResult());
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(loadService, times(1)).loadInvoice(any());
        verify(syncErrorRepository).save(argThat(err -> "PIPELINE_ERROR".equals(err.getErrorType())));
    }

    @Test
    void runFullPipeline_loadFailure_shouldLogErrorAndContinue() {
        when(invoiceIntegrationService.syncInvoices()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawInvoiceRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawInvoice("ACC-001", "{\"id\":\"ACC-001\"}"),
                buildRawInvoice("ACC-002", "{\"id\":\"ACC-002\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("ACC-001"));
        when(validationService.validate(any())).thenReturn(validResult());
        doThrow(new RuntimeException("DB error")).doNothing().when(loadService).loadInvoice(any());
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(syncErrorRepository).save(argThat(err -> "PIPELINE_ERROR".equals(err.getErrorType())));
    }

    @Test
    void runFullPipeline_emptyStaging_shouldCompleteWithZero() {
        when(invoiceIntegrationService.syncInvoices()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawInvoiceRepository.findBySyncJobId(1L)).thenReturn(Collections.emptyList());
        when(syncJobService.completeJob(any(), eq(0), eq(0))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(loadService, never()).loadInvoice(any());
        verify(syncJobService).completeJob(runningJob, 0, 0);
    }

    private RawInvoice buildRawInvoice(String externalId, String rawData) {
        return RawInvoice.builder().id(1L).syncJob(runningJob)
                .externalId(externalId).rawData(rawData).build();
    }

    private TransformedInvoice buildTransformed(String externalId) {
        return TransformedInvoice.builder().externalId(externalId).invoiceNumber("INV-001")
                .customerName("Acme Corp").amount(BigDecimal.valueOf(1500.50)).currency("USD")
                .status("paid").dueDate(LocalDate.of(2025, 6, 15)).build();
    }

    private ValidationResult validResult() {
        return ValidationResult.builder().valid(true).errors(Collections.emptyList()).build();
    }
}
