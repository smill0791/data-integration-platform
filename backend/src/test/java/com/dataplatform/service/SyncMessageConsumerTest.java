package com.dataplatform.service;

import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.dto.SyncMessage;
import com.dataplatform.model.SyncJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncMessageConsumerTest {

    @Mock
    private CustomerPipelineService customerPipelineService;

    @Mock
    private SyncJobService syncJobService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SyncMessageConsumer consumer;

    @Test
    void handleSyncMessage_shouldDeserializeAndRunPipeline() throws Exception {
        SyncMessage message = SyncMessage.builder()
                .jobId(1L).sourceName("CRM").syncType("FULL").build();
        String json = objectMapper.writeValueAsString(message);

        SyncJob startedJob = SyncJob.builder().id(1L).status("RUNNING")
                .startTime(LocalDateTime.now()).build();
        when(syncJobService.startJob(1L)).thenReturn(startedJob);
        when(customerPipelineService.runPipelineForJob(1L))
                .thenReturn(SyncJobDTO.builder().id(1L).status("COMPLETED").build());

        consumer.handleSyncMessage(json);

        verify(syncJobService).startJob(1L);
        verify(customerPipelineService).runPipelineForJob(1L);
    }

    @Test
    void handleSyncMessage_whenPipelineFails_shouldFailJobAndRethrow() throws Exception {
        SyncMessage message = SyncMessage.builder()
                .jobId(2L).sourceName("CRM").syncType("FULL").build();
        String json = objectMapper.writeValueAsString(message);

        SyncJob startedJob = SyncJob.builder().id(2L).status("RUNNING")
                .startTime(LocalDateTime.now()).build();
        when(syncJobService.startJob(2L)).thenReturn(startedJob);
        when(customerPipelineService.runPipelineForJob(2L))
                .thenThrow(new RuntimeException("Pipeline exploded"));
        when(syncJobService.getJobEntity(2L)).thenReturn(startedJob);
        when(syncJobService.failJob(any(), any())).thenReturn(startedJob);

        assertThatThrownBy(() -> consumer.handleSyncMessage(json))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQS message processing failed");

        verify(syncJobService).failJob(eq(startedJob), contains("Pipeline exploded"));
    }

    @Test
    void handleSyncMessage_whenInvalidJson_shouldRethrow() {
        assertThatThrownBy(() -> consumer.handleSyncMessage("not json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SQS message processing failed");
    }
}
