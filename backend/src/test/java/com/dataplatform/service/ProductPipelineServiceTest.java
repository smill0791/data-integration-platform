package com.dataplatform.service;

import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.dto.TransformedProduct;
import com.dataplatform.dto.ValidationResult;
import com.dataplatform.model.RawProduct;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawProductRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.transformer.ProductTransformationService;
import com.dataplatform.validator.ProductValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductPipelineServiceTest {

    @Mock private ProductIntegrationService productIntegrationService;
    @Mock private ProductTransformationService transformationService;
    @Mock private ProductValidationService validationService;
    @Mock private ProductLoadService loadService;
    @Mock private SyncJobService syncJobService;
    @Mock private RawProductRepository rawProductRepository;
    @Mock private SyncErrorRepository syncErrorRepository;

    @InjectMocks
    private ProductPipelineService pipelineService;

    private SyncJob runningJob;
    private SyncJobDTO stagingResult;

    @BeforeEach
    void setUp() {
        runningJob = SyncJob.builder()
                .id(1L).sourceName("ERP").syncType("FULL").status("COMPLETED")
                .startTime(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .recordsProcessed(2).recordsFailed(0).build();

        stagingResult = SyncJobDTO.builder()
                .id(1L).sourceName("ERP").syncType("FULL").status("COMPLETED")
                .recordsProcessed(2).recordsFailed(0).build();
    }

    @Test
    void runFullPipeline_allSuccess_shouldLoadAllRecords() {
        when(productIntegrationService.syncProducts()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawProductRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawProduct("ERP-001", "{\"id\":\"ERP-001\",\"name\":\"Widget\"}"),
                buildRawProduct("ERP-002", "{\"id\":\"ERP-002\",\"name\":\"Gadget\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("ERP-001", "Widget"));
        when(validationService.validate(any())).thenReturn(validResult());
        when(syncJobService.completeJob(any(), eq(2), eq(0))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(loadService, times(2)).loadProduct(any());
        verify(syncJobService).completeJob(runningJob, 2, 0);
    }

    @Test
    void runFullPipeline_stagingFails_shouldReturnEarly() {
        SyncJobDTO failedStaging = SyncJobDTO.builder().id(1L).status("FAILED").build();
        when(productIntegrationService.syncProducts()).thenReturn(failedStaging);

        SyncJobDTO result = pipelineService.runFullPipeline();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(loadService, never()).loadProduct(any());
    }

    @Test
    void runFullPipeline_validationFailure_shouldLogErrorAndContinue() {
        when(productIntegrationService.syncProducts()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawProductRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawProduct("ERP-001", "{\"id\":\"ERP-001\"}"),
                buildRawProduct("ERP-002", "{\"id\":\"ERP-002\",\"name\":\"Gadget\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("ERP-001", null));
        ValidationResult invalid = ValidationResult.builder().valid(false).errors(List.of("name is required")).build();
        when(validationService.validate(any())).thenReturn(invalid, validResult());
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(loadService, times(1)).loadProduct(any());
        verify(syncErrorRepository).save(argThat(err -> "VALIDATION_ERROR".equals(err.getErrorType())));
    }

    @Test
    void runFullPipeline_transformFailure_shouldLogErrorAndContinue() {
        when(productIntegrationService.syncProducts()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawProductRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawProduct("ERP-001", "bad json"),
                buildRawProduct("ERP-002", "{\"id\":\"ERP-002\",\"name\":\"Gadget\"}")
        ));
        when(transformationService.transform("bad json")).thenThrow(new IllegalArgumentException("Failed to parse"));
        when(transformationService.transform("{\"id\":\"ERP-002\",\"name\":\"Gadget\"}"))
                .thenReturn(buildTransformed("ERP-002", "Gadget"));
        when(validationService.validate(any())).thenReturn(validResult());
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(loadService, times(1)).loadProduct(any());
        verify(syncErrorRepository).save(argThat(err -> "PIPELINE_ERROR".equals(err.getErrorType())));
    }

    @Test
    void runFullPipeline_loadFailure_shouldLogErrorAndContinue() {
        when(productIntegrationService.syncProducts()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawProductRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawProduct("ERP-001", "{\"id\":\"ERP-001\",\"name\":\"Widget\"}"),
                buildRawProduct("ERP-002", "{\"id\":\"ERP-002\",\"name\":\"Gadget\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("ERP-001", "Widget"));
        when(validationService.validate(any())).thenReturn(validResult());
        doThrow(new RuntimeException("DB error")).doNothing().when(loadService).loadProduct(any());
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(syncErrorRepository).save(argThat(err -> "PIPELINE_ERROR".equals(err.getErrorType())));
    }

    @Test
    void runFullPipeline_emptyStaging_shouldCompleteWithZero() {
        when(productIntegrationService.syncProducts()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawProductRepository.findBySyncJobId(1L)).thenReturn(Collections.emptyList());
        when(syncJobService.completeJob(any(), eq(0), eq(0))).thenReturn(runningJob);

        pipelineService.runFullPipeline();

        verify(loadService, never()).loadProduct(any());
        verify(syncJobService).completeJob(runningJob, 0, 0);
    }

    private RawProduct buildRawProduct(String externalId, String rawData) {
        return RawProduct.builder().id(1L).syncJob(runningJob)
                .externalId(externalId).rawData(rawData).build();
    }

    private TransformedProduct buildTransformed(String externalId, String name) {
        return TransformedProduct.builder().externalId(externalId).sku("SKU001")
                .name(name).unitPrice(BigDecimal.valueOf(29.99)).quantity(50).build();
    }

    private ValidationResult validResult() {
        return ValidationResult.builder().valid(true).errors(Collections.emptyList()).build();
    }
}
