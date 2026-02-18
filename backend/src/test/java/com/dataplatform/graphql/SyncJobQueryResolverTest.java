package com.dataplatform.graphql;

import com.dataplatform.model.RawCustomer;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawCustomerRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncJobQueryResolverTest {

    @Mock
    private SyncJobRepository syncJobRepository;
    @Mock
    private SyncErrorRepository syncErrorRepository;
    @Mock
    private RawCustomerRepository rawCustomerRepository;

    @InjectMocks
    private SyncJobQueryResolver resolver;

    private SyncJob completedJob;
    private SyncJob failedJob;

    @BeforeEach
    void setUp() {
        completedJob = SyncJob.builder()
                .id(1L).sourceName("CRM").syncType("FULL").status("COMPLETED")
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .recordsProcessed(100).recordsFailed(2)
                .build();

        failedJob = SyncJob.builder()
                .id(2L).sourceName("ERP").syncType("FULL").status("FAILED")
                .startTime(LocalDateTime.now().minusMinutes(10))
                .endTime(LocalDateTime.now().minusMinutes(9))
                .recordsProcessed(0).recordsFailed(5)
                .build();
    }

    @Test
    void syncJob_shouldReturnJobById() {
        when(syncJobRepository.findById(1L)).thenReturn(Optional.of(completedJob));
        SyncJob result = resolver.syncJob(1L);
        assertThat(result).isEqualTo(completedJob);
    }

    @Test
    void syncJob_whenNotFound_shouldReturnNull() {
        when(syncJobRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(resolver.syncJob(99L)).isNull();
    }

    @Test
    void syncJobs_shouldReturnAllJobsWithDefaults() {
        when(syncJobRepository.findAllByOrderByStartTimeDesc())
                .thenReturn(List.of(completedJob, failedJob));

        List<SyncJob> result = resolver.syncJobs(20, 0, null, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void syncJobs_shouldFilterByStatus() {
        when(syncJobRepository.findAllByOrderByStartTimeDesc())
                .thenReturn(List.of(completedJob, failedJob));

        List<SyncJob> result = resolver.syncJobs(20, 0, Map.of("status", "COMPLETED"), null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void syncJobs_shouldApplyLimitAndOffset() {
        when(syncJobRepository.findAllByOrderByStartTimeDesc())
                .thenReturn(List.of(completedJob, failedJob));

        List<SyncJob> result = resolver.syncJobs(1, 1, null, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void duration_shouldComputeSeconds() {
        Integer duration = resolver.duration(completedJob);
        assertThat(duration).isEqualTo(300); // 5 minutes
    }

    @Test
    void duration_whenNoEndTime_shouldReturnNull() {
        SyncJob running = SyncJob.builder().id(3L).startTime(LocalDateTime.now()).build();
        assertThat(resolver.duration(running)).isNull();
    }

    @Test
    void successRate_shouldComputePercentage() {
        Double rate = resolver.successRate(completedJob);
        assertThat(rate).isCloseTo(98.04, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void errors_shouldReturnLimitedErrors() {
        SyncError error = SyncError.builder()
                .id(1L).errorType("VALIDATION_ERROR").errorMessage("bad data")
                .occurredAt(LocalDateTime.now()).build();
        when(syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(1L))
                .thenReturn(List.of(error));

        List<SyncError> result = resolver.errors(completedJob, 5);
        assertThat(result).hasSize(1);
    }

    @Test
    void syncMetrics_shouldComputeAggregates() {
        when(syncJobRepository.findAllByOrderByStartTimeDesc())
                .thenReturn(List.of(completedJob, failedJob));

        Map<String, Object> metrics = resolver.syncMetrics("LAST_30_DAYS");

        assertThat(metrics).containsKeys("last24Hours", "last30Days");
        @SuppressWarnings("unchecked")
        Map<String, Object> last24h = (Map<String, Object>) metrics.get("last24Hours");
        assertThat((int) last24h.get("totalSyncs")).isEqualTo(2);
    }
}
