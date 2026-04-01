package com.dataplatform.integration;

import com.dataplatform.dto.SalesforceTokenResponse;
import com.dataplatform.exception.IntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesforceAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SalesforceAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new SalesforceAuthService(
                restTemplate,
                "https://login.salesforce.com",
                "test-client-id",
                "test-client-secret");
    }

    @Test
    void getAccessToken_shouldFetchAndCacheToken() {
        SalesforceTokenResponse tokenResponse = SalesforceTokenResponse.builder()
                .accessToken("access-token-123")
                .instanceUrl("https://myorg.my.salesforce.com")
                .tokenType("Bearer")
                .build();
        when(restTemplate.postForObject(
                eq("https://login.salesforce.com/services/oauth2/token"),
                any(), eq(SalesforceTokenResponse.class)))
                .thenReturn(tokenResponse);

        String token = authService.getAccessToken();
        assertThat(token).isEqualTo("access-token-123");

        // Second call should use cache
        String token2 = authService.getAccessToken();
        assertThat(token2).isEqualTo("access-token-123");

        verify(restTemplate, times(1)).postForObject(any(String.class), any(), any());
    }

    @Test
    void getInstanceUrl_shouldReturnCachedInstanceUrl() {
        SalesforceTokenResponse tokenResponse = SalesforceTokenResponse.builder()
                .accessToken("token")
                .instanceUrl("https://myorg.my.salesforce.com")
                .build();
        when(restTemplate.postForObject(any(String.class), any(), eq(SalesforceTokenResponse.class)))
                .thenReturn(tokenResponse);

        String instanceUrl = authService.getInstanceUrl();
        assertThat(instanceUrl).isEqualTo("https://myorg.my.salesforce.com");
    }

    @Test
    void refreshToken_shouldClearCacheAndFetchNewToken() {
        SalesforceTokenResponse tokenResponse1 = SalesforceTokenResponse.builder()
                .accessToken("old-token")
                .instanceUrl("https://myorg.my.salesforce.com")
                .build();
        SalesforceTokenResponse tokenResponse2 = SalesforceTokenResponse.builder()
                .accessToken("new-token")
                .instanceUrl("https://myorg.my.salesforce.com")
                .build();
        when(restTemplate.postForObject(any(String.class), any(), eq(SalesforceTokenResponse.class)))
                .thenReturn(tokenResponse1, tokenResponse2);

        authService.getAccessToken();
        String refreshed = authService.refreshToken();

        assertThat(refreshed).isEqualTo("new-token");
        verify(restTemplate, times(2)).postForObject(any(String.class), any(), any());
    }

    @Test
    void getAccessToken_whenRestClientFails_shouldThrowIntegrationException() {
        when(restTemplate.postForObject(any(String.class), any(), eq(SalesforceTokenResponse.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThatThrownBy(() -> authService.getAccessToken())
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Failed to obtain Salesforce OAuth token");
    }

    @Test
    void getAccessToken_whenNullResponse_shouldThrowIntegrationException() {
        when(restTemplate.postForObject(any(String.class), any(), eq(SalesforceTokenResponse.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> authService.getAccessToken())
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("missing access_token");
    }
}
