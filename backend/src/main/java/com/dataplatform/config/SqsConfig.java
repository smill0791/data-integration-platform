package com.dataplatform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.net.URI;
import java.util.Map;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class SqsConfig {

    @Value("${spring.cloud.aws.sqs.endpoint}")
    private String sqsEndpoint;

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Value("${app.sqs.customer-sync-queue}")
    private String customerSyncQueue;

    @Value("${app.sqs.customer-sync-dlq}")
    private String customerSyncDlq;

    @Value("${app.sqs.max-retries}")
    private int maxRetries;

    @Bean
    public SqsClient sqsClient() {
        SqsClient client = SqsClient.builder()
                .endpointOverride(URI.create(sqsEndpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        initializeQueues(client);
        return client;
    }

    private void initializeQueues(SqsClient client) {
        try {
            // Create DLQ first
            CreateQueueResponse dlqResponse = client.createQueue(CreateQueueRequest.builder()
                    .queueName(customerSyncDlq)
                    .build());
            String dlqArn = client.getQueueAttributes(GetQueueAttributesRequest.builder()
                    .queueUrl(dlqResponse.queueUrl())
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build())
                    .attributes().get(QueueAttributeName.QUEUE_ARN);

            // Create main queue with redrive policy pointing to DLQ
            String redrivePolicy = String.format(
                    "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"%d\"}",
                    dlqArn, maxRetries);

            client.createQueue(CreateQueueRequest.builder()
                    .queueName(customerSyncQueue)
                    .attributes(Map.of(QueueAttributeName.REDRIVE_POLICY, redrivePolicy))
                    .build());

            log.info("SQS queues initialized: {} (DLQ: {})", customerSyncQueue, customerSyncDlq);
        } catch (Exception e) {
            log.warn("Failed to initialize SQS queues (LocalStack may not be running): {}", e.getMessage());
        }
    }
}
