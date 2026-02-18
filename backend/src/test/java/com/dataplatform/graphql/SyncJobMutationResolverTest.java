package com.dataplatform.graphql;

import com.dataplatform.model.SyncJob;
import com.dataplatform.service.SyncJobService;
import com.dataplatform.service.SyncMessageProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncJobMutationResolverTest {

    @Mock
    private SyncJobService syncJobService;
    @Mock
    private SyncMessageProducer syncMessageProducer;

    @InjectMocks
    private SyncJobMutationResolver resolver;

    @Test
    void triggerSync_shouldCreateQueuedJobAndSendMessage() {
        SyncJob queuedJob = SyncJob.builder().id(1L).sourceName("CRM").syncType("FULL")
                .status("QUEUED").startTime(LocalDateTime.now()).build();

        when(syncJobService.createQueuedJob("CRM", "FULL")).thenReturn(queuedJob);

        SyncJob result = resolver.triggerSync(Map.of("sourceName", "CRM"));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("QUEUED");
        verify(syncMessageProducer).sendSyncRequest(1L, "CRM", "FULL");
    }

    @Test
    void cancelSync_whenRunning_shouldFailJob() {
        SyncJob running = SyncJob.builder().id(1L).status("RUNNING")
                .startTime(LocalDateTime.now()).build();
        SyncJob cancelled = SyncJob.builder().id(1L).status("FAILED")
                .startTime(LocalDateTime.now()).endTime(LocalDateTime.now()).build();

        when(syncJobService.getJobEntity(1L)).thenReturn(running);
        when(syncJobService.failJob(running, "Cancelled via GraphQL")).thenReturn(cancelled);

        SyncJob result = resolver.cancelSync(1L);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void cancelSync_whenQueued_shouldFailJob() {
        SyncJob queued = SyncJob.builder().id(1L).status("QUEUED")
                .startTime(LocalDateTime.now()).build();
        SyncJob cancelled = SyncJob.builder().id(1L).status("FAILED")
                .startTime(LocalDateTime.now()).endTime(LocalDateTime.now()).build();

        when(syncJobService.getJobEntity(1L)).thenReturn(queued);
        when(syncJobService.failJob(queued, "Cancelled via GraphQL")).thenReturn(cancelled);

        SyncJob result = resolver.cancelSync(1L);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void cancelSync_whenAlreadyCompleted_shouldReturnAsIs() {
        SyncJob completed = SyncJob.builder().id(1L).status("COMPLETED")
                .startTime(LocalDateTime.now()).build();

        when(syncJobService.getJobEntity(1L)).thenReturn(completed);

        SyncJob result = resolver.cancelSync(1L);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(syncJobService, never()).failJob(any(), any());
    }
}
