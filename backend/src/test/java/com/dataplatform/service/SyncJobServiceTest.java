package com.dataplatform.service;

import com.dataplatform.dto.SyncErrorDTO;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.exception.ResourceNotFoundException;
import com.dataplatform.graphql.SyncJobEventPublisher;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.repository.SyncJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncJobServiceTest {

    @Mock
    private SyncJobRepository syncJobRepository;

    @Mock
    private SyncErrorRepository syncErrorRepository;

    @Mock
    private SyncJobEventPublisher eventPublisher;

    @InjectMocks
    private SyncJobService syncJobService;

    private SyncJob sampleJob;

    @BeforeEach
    void setUp() {
        sampleJob = SyncJob.builder()
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

    @Test
    void createJob_shouldCreateRunningJob() {
        when(syncJobRepository.save(any(SyncJob.class))).thenAnswer(inv -> {
            SyncJob job = inv.getArgument(0);
            job.setId(1L);
            return job;
        });

        SyncJob result = syncJobService.createJob("CRM", "FULL");

        assertThat(result.getSourceName()).isEqualTo("CRM");
        assertThat(result.getSyncType()).isEqualTo("FULL");
        assertThat(result.getStatus()).isEqualTo("RUNNING");
        assertThat(result.getStartTime()).isNotNull();
        verify(syncJobRepository).save(any(SyncJob.class));
    }

    @Test
    void completeJob_withProcessedRecords_shouldSetCompleted() {
        when(syncJobRepository.save(any(SyncJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncJob result = syncJobService.completeJob(sampleJob, 100, 5);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRecordsProcessed()).isEqualTo(100);
        assertThat(result.getRecordsFailed()).isEqualTo(5);
        assertThat(result.getEndTime()).isNotNull();
    }

    @Test
    void completeJob_withAllFailed_shouldSetFailed() {
        when(syncJobRepository.save(any(SyncJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncJob result = syncJobService.completeJob(sampleJob, 0, 10);

        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void failJob_shouldSetFailedWithMessage() {
        when(syncJobRepository.save(any(SyncJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncJob result = syncJobService.failJob(sampleJob, "Connection refused");

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).isEqualTo("Connection refused");
        assertThat(result.getEndTime()).isNotNull();
    }

    @Test
    void getJobById_whenExists_shouldReturnDTO() {
        when(syncJobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        SyncJobDTO dto = syncJobService.getJobById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getSourceName()).isEqualTo("CRM");
        assertThat(dto.getStatus()).isEqualTo("RUNNING");
    }

    @Test
    void getJobById_whenNotFound_shouldThrow() {
        when(syncJobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syncJobService.getJobById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getRecentJobs_shouldReturnDTOList() {
        when(syncJobRepository.findTop20ByOrderByStartTimeDesc())
                .thenReturn(List.of(sampleJob));

        List<SyncJobDTO> result = syncJobService.getRecentJobs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSourceName()).isEqualTo("CRM");
    }

    @Test
    void getErrorsForJob_whenJobExists_shouldReturnMappedDTOs() {
        when(syncJobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        SyncError error = SyncError.builder()
                .id(10L)
                .syncJob(sampleJob)
                .errorType("VALIDATION_ERROR")
                .errorMessage("Missing required field: name")
                .failedRecord("{\"id\": \"ext-1\"}")
                .occurredAt(LocalDateTime.now())
                .build();
        when(syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(1L))
                .thenReturn(List.of(error));

        List<SyncErrorDTO> result = syncJobService.getErrorsForJob(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getErrorType()).isEqualTo("VALIDATION_ERROR");
        assertThat(result.get(0).getErrorMessage()).isEqualTo("Missing required field: name");
        assertThat(result.get(0).getFailedRecord()).isEqualTo("{\"id\": \"ext-1\"}");
    }

    @Test
    void getErrorsForJob_whenJobNotFound_shouldThrow() {
        when(syncJobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syncJobService.getErrorsForJob(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createJob_shouldPublishEvent() {
        when(syncJobRepository.save(any(SyncJob.class))).thenAnswer(inv -> {
            SyncJob job = inv.getArgument(0);
            job.setId(1L);
            return job;
        });

        syncJobService.createJob("CRM", "FULL");

        verify(eventPublisher).publish(any(SyncJob.class));
    }
}
