package com.dataplatform.service;

import com.dataplatform.dto.SyncMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncMessageProducerTest {

    @Mock
    private SqsClient sqsClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SyncMessageProducer producer;

    @BeforeEach
    void setUp() throws Exception {
        producer = new SyncMessageProducer(sqsClient, objectMapper);
        Field queueNameField = SyncMessageProducer.class.getDeclaredField("queueName");
        queueNameField.setAccessible(true);
        queueNameField.set(producer, "customer-sync-queue");
    }

    @Test
    void sendSyncRequest_shouldSerializeAndSend() throws Exception {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder()
                        .queueUrl("http://localhost:4566/000000000000/customer-sync-queue")
                        .build());
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

        producer.sendSyncRequest(1L, "CRM", "FULL");

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        String body = captor.getValue().messageBody();
        SyncMessage parsed = objectMapper.readValue(body, SyncMessage.class);
        assertThat(parsed.getJobId()).isEqualTo(1L);
        assertThat(parsed.getSourceName()).isEqualTo("CRM");
        assertThat(parsed.getSyncType()).isEqualTo("FULL");
    }

    @Test
    void sendSyncRequest_whenSqsFails_shouldThrow() {
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenThrow(new RuntimeException("SQS unavailable"));

        assertThatThrownBy(() -> producer.sendSyncRequest(1L, "CRM", "FULL"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to queue sync request");
    }
}
