package com.dataplatform.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@ConditionalOnProperty(name = "app.sqs.enabled", havingValue = "false")
public class TestSqsConfig {

    @Bean
    @Primary
    public SqsClient sqsClient() {
        return Mockito.mock(SqsClient.class);
    }
}
