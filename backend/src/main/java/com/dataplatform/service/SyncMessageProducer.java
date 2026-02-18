package com.dataplatform.service;

import com.dataplatform.dto.SyncMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncMessageProducer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${app.sqs.customer-sync-queue}")
    private String queueName;

    public void sendSyncRequest(Long jobId, String sourceName, String syncType) {
        try {
            SyncMessage message = SyncMessage.builder()
                    .jobId(jobId)
                    .sourceName(sourceName)
                    .syncType(syncType)
                    .build();

            String messageBody = objectMapper.writeValueAsString(message);

            String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build()).queueUrl();

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build());

            log.info("Sent sync request to SQS: jobId={}, source={}, type={}", jobId, sourceName, syncType);
        } catch (Exception e) {
            log.error("Failed to send sync request to SQS for job {}: {}", jobId, e.getMessage(), e);
            throw new RuntimeException("Failed to queue sync request", e);
        }
    }
}
