package com.dataplatform.service;

import com.dataplatform.dto.SyncMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.sqs.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SyncMessageConsumer {

    private final CustomerPipelineService customerPipelineService;
    private final SyncJobService syncJobService;
    private final ObjectMapper objectMapper;

    @SqsListener("${app.sqs.customer-sync-queue}")
    public void handleSyncMessage(String messageBody) {
        SyncMessage message = null;
        try {
            message = objectMapper.readValue(messageBody, SyncMessage.class);
            log.info("Received sync message from SQS: jobId={}, source={}", message.getJobId(), message.getSourceName());

            syncJobService.startJob(message.getJobId());
            customerPipelineService.runPipelineForJob(message.getJobId());

            log.info("Successfully processed sync message for job {}", message.getJobId());
        } catch (Exception e) {
            Long jobId = message != null ? message.getJobId() : null;
            log.error("Failed to process sync message for job {}: {}", jobId, e.getMessage(), e);
            if (jobId != null) {
                try {
                    syncJobService.failJob(syncJobService.getJobEntity(jobId), "SQS processing failed: " + e.getMessage());
                } catch (Exception failEx) {
                    log.error("Failed to mark job {} as failed: {}", jobId, failEx.getMessage());
                }
            }
            throw new RuntimeException("SQS message processing failed", e);
        }
    }
}
