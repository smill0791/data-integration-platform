package com.dataplatform.service;

import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.dto.TransformedCustomer;
import com.dataplatform.dto.ValidationResult;
import com.dataplatform.model.RawCustomer;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawCustomerRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.transformer.CustomerTransformationService;
import com.dataplatform.validator.CustomerValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerPipelineServiceTest {

    @Mock
    private CustomerIntegrationService customerIntegrationService;

    @Mock
    private CustomerTransformationService transformationService;

    @Mock
    private CustomerValidationService validationService;

    @Mock
    private CustomerLoadService loadService;

    @Mock
    private SyncJobService syncJobService;

    @Mock
    private RawCustomerRepository rawCustomerRepository;

    @Mock
    private SyncErrorRepository syncErrorRepository;

    @InjectMocks
    private CustomerPipelineService pipelineService;

    private SyncJob runningJob;
    private SyncJobDTO stagingResult;

    @BeforeEach
    void setUp() {
        runningJob = SyncJob.builder()
                .id(1L)
                .sourceName("CRM")
                .syncType("FULL")
                .status("COMPLETED")
                .startTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .recordsProcessed(2)
                .recordsFailed(0)
                .build();

        stagingResult = SyncJobDTO.builder()
                .id(1L)
                .sourceName("CRM")
                .syncType("FULL")
                .status("COMPLETED")
                .recordsProcessed(2)
                .recordsFailed(0)
                .build();
    }

    @Test
    void runFullPipeline_allSuccess_shouldLoadAllRecords() {
        when(customerIntegrationService.syncCustomers()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawCustomerRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawCustomer("CRM-001", "{\"id\":\"CRM-001\",\"name\":\"Alice\"}"),
                buildRawCustomer("CRM-002", "{\"id\":\"CRM-002\",\"name\":\"Bob\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("CRM-001", "Alice"));
        when(validationService.validate(any())).thenReturn(validResult());
        when(syncJobService.completeJob(any(), eq(2), eq(0))).thenAnswer(inv -> {
            runningJob.setRecordsProcessed(2);
            runningJob.setRecordsFailed(0);
            return runningJob;
        });

        SyncJobDTO result = pipelineService.runFullPipeline();

        verify(loadService, times(2)).loadCustomer(any());
        verify(syncJobService).completeJob(runningJob, 2, 0);
        verify(syncErrorRepository, never()).save(any());
    }

    @Test
    void runFullPipeline_stagingFails_shouldReturnEarly() {
        SyncJobDTO failedStaging = SyncJobDTO.builder()
                .id(1L)
                .status("FAILED")
                .errorMessage("CRM API down")
                .build();
        when(customerIntegrationService.syncCustomers()).thenReturn(failedStaging);

        SyncJobDTO result = pipelineService.runFullPipeline();

        assertThat(result.getStatus()).isEqualTo("FAILED");
        verify(syncJobService, never()).getJobEntity(any());
        verify(loadService, never()).loadCustomer(any());
    }

    @Test
    void runFullPipeline_validationFailure_shouldLogErrorAndContinue() {
        when(customerIntegrationService.syncCustomers()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawCustomerRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawCustomer("CRM-001", "{\"id\":\"CRM-001\"}"),
                buildRawCustomer("CRM-002", "{\"id\":\"CRM-002\",\"name\":\"Bob\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("CRM-001", null));
        ValidationResult invalid = ValidationResult.builder()
                .valid(false)
                .errors(List.of("name is required"))
                .build();
        ValidationResult valid = validResult();
        when(validationService.validate(any())).thenReturn(invalid, valid);
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenAnswer(inv -> {
            runningJob.setRecordsProcessed(1);
            runningJob.setRecordsFailed(1);
            return runningJob;
        });

        SyncJobDTO result = pipelineService.runFullPipeline();

        verify(loadService, times(1)).loadCustomer(any());
        verify(syncErrorRepository).save(argThat(err -> "VALIDATION_ERROR".equals(err.getErrorType())));
        verify(syncJobService).completeJob(runningJob, 1, 1);
    }

    @Test
    void runFullPipeline_transformFailure_shouldLogErrorAndContinue() {
        when(customerIntegrationService.syncCustomers()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawCustomerRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawCustomer("CRM-001", "bad json"),
                buildRawCustomer("CRM-002", "{\"id\":\"CRM-002\",\"name\":\"Bob\"}")
        ));
        when(transformationService.transform("bad json"))
                .thenThrow(new IllegalArgumentException("Failed to parse"));
        when(transformationService.transform("{\"id\":\"CRM-002\",\"name\":\"Bob\"}"))
                .thenReturn(buildTransformed("CRM-002", "Bob"));
        when(validationService.validate(any())).thenReturn(validResult());
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenAnswer(inv -> {
            runningJob.setRecordsProcessed(1);
            runningJob.setRecordsFailed(1);
            return runningJob;
        });

        SyncJobDTO result = pipelineService.runFullPipeline();

        verify(loadService, times(1)).loadCustomer(any());
        verify(syncErrorRepository).save(argThat(err -> "PIPELINE_ERROR".equals(err.getErrorType())));
        verify(syncJobService).completeJob(runningJob, 1, 1);
    }

    @Test
    void runFullPipeline_loadFailure_shouldLogErrorAndContinue() {
        when(customerIntegrationService.syncCustomers()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawCustomerRepository.findBySyncJobId(1L)).thenReturn(List.of(
                buildRawCustomer("CRM-001", "{\"id\":\"CRM-001\",\"name\":\"Alice\"}"),
                buildRawCustomer("CRM-002", "{\"id\":\"CRM-002\",\"name\":\"Bob\"}")
        ));
        when(transformationService.transform(anyString())).thenReturn(buildTransformed("CRM-001", "Alice"));
        when(validationService.validate(any())).thenReturn(validResult());
        doThrow(new RuntimeException("DB error")).doNothing().when(loadService).loadCustomer(any());
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncJobService.completeJob(any(), eq(1), eq(1))).thenAnswer(inv -> {
            runningJob.setRecordsProcessed(1);
            runningJob.setRecordsFailed(1);
            return runningJob;
        });

        SyncJobDTO result = pipelineService.runFullPipeline();

        verify(syncErrorRepository).save(argThat(err -> "PIPELINE_ERROR".equals(err.getErrorType())));
        verify(syncJobService).completeJob(runningJob, 1, 1);
    }

    @Test
    void runFullPipeline_emptyStaging_shouldCompleteWithZero() {
        when(customerIntegrationService.syncCustomers()).thenReturn(stagingResult);
        when(syncJobService.getJobEntity(1L)).thenReturn(runningJob);
        when(rawCustomerRepository.findBySyncJobId(1L)).thenReturn(Collections.emptyList());
        when(syncJobService.completeJob(any(), eq(0), eq(0))).thenAnswer(inv -> {
            runningJob.setRecordsProcessed(0);
            runningJob.setRecordsFailed(0);
            return runningJob;
        });

        SyncJobDTO result = pipelineService.runFullPipeline();

        verify(loadService, never()).loadCustomer(any());
        verify(syncJobService).completeJob(runningJob, 0, 0);
    }

    private RawCustomer buildRawCustomer(String externalId, String rawData) {
        return RawCustomer.builder()
                .id(1L)
                .syncJob(runningJob)
                .externalId(externalId)
                .rawData(rawData)
                .build();
    }

    private TransformedCustomer buildTransformed(String externalId, String name) {
        return TransformedCustomer.builder()
                .externalId(externalId)
                .name(name)
                .email("test@example.com")
                .phone("5551234567")
                .address("123 Main St, Springfield, IL 62701")
                .build();
    }

    private ValidationResult validResult() {
        return ValidationResult.builder()
                .valid(true)
                .errors(Collections.emptyList())
                .build();
    }
}
