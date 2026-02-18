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

    @Mock private CustomerPipelineService customerPipelineService;
    @Mock private ProductPipelineService productPipelineService;
    @Mock private InvoicePipelineService invoicePipelineService;
    @Mock private SyncJobService syncJobService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SyncMessageConsumer consumer;

    @Test
    void handleSyncMessage_crmSource_shouldRouteToCustomerPipeline() throws Exception {
        SyncMessage message = SyncMessage.builder().jobId(1L).sourceName("CRM").syncType("FULL").build();
        String json = objectMapper.writeValueAsString(message);
        when(syncJobService.startJob(1L)).thenReturn(SyncJob.builder().id(1L).status("RUNNING").startTime(LocalDateTime.now()).build());
        when(customerPipelineService.runPipelineForJob(1L)).thenReturn(SyncJobDTO.builder().id(1L).status("COMPLETED").build());

        consumer.handleSyncMessage(json);

        verify(customerPipelineService).runPipelineForJob(1L);
        verify(productPipelineService, never()).runPipelineForJob(any());
        verify(invoicePipelineService, never()).runPipelineForJob(any());
    }

    @Test
    void handleSyncMessage_erpSource_shouldRouteToProductPipeline() throws Exception {
        SyncMessage message = SyncMessage.builder().jobId(2L).sourceName("ERP").syncType("FULL").build();
        String json = objectMapper.writeValueAsString(message);
        when(syncJobService.startJob(2L)).thenReturn(SyncJob.builder().id(2L).status("RUNNING").startTime(LocalDateTime.now()).build());
        when(productPipelineService.runPipelineForJob(2L)).thenReturn(SyncJobDTO.builder().id(2L).status("COMPLETED").build());

        consumer.handleSyncMessage(json);

        verify(productPipelineService).runPipelineForJob(2L);
        verify(customerPipelineService, never()).runPipelineForJob(any());
        verify(invoicePipelineService, never()).runPipelineForJob(any());
    }

    @Test
    void handleSyncMessage_accountingSource_shouldRouteToInvoicePipeline() throws Exception {
        SyncMessage message = SyncMessage.builder().jobId(3L).sourceName("ACCOUNTING").syncType("FULL").build();
        String json = objectMapper.writeValueAsString(message);
        when(syncJobService.startJob(3L)).thenReturn(SyncJob.builder().id(3L).status("RUNNING").startTime(LocalDateTime.now()).build());
        when(invoicePipelineService.runPipelineForJob(3L)).thenReturn(SyncJobDTO.builder().id(3L).status("COMPLETED").build());

        consumer.handleSyncMessage(json);

        verify(invoicePipelineService).runPipelineForJob(3L);
        verify(customerPipelineService, never()).runPipelineForJob(any());
        verify(productPipelineService, never()).runPipelineForJob(any());
    }

    @Test
    void handleSyncMessage_whenPipelineFails_shouldFailJobAndRethrow() throws Exception {
        SyncMessage message = SyncMessage.builder().jobId(4L).sourceName("CRM").syncType("FULL").build();
        String json = objectMapper.writeValueAsString(message);
        SyncJob startedJob = SyncJob.builder().id(4L).status("RUNNING").startTime(LocalDateTime.now()).build();
        when(syncJobService.startJob(4L)).thenReturn(startedJob);
        when(customerPipelineService.runPipelineForJob(4L)).thenThrow(new RuntimeException("Pipeline exploded"));
        when(syncJobService.getJobEntity(4L)).thenReturn(startedJob);
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
