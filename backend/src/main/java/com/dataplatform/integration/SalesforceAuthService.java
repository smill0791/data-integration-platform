package com.dataplatform.integration;

import com.dataplatform.dto.SalesforceTokenResponse;
import com.dataplatform.exception.IntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SalesforceAuthService {

    private final RestTemplate restTemplate;
    private final String loginUrl;
    private final String clientId;
    private final String clientSecret;

    private String cachedAccessToken;
    private String cachedInstanceUrl;

    public SalesforceAuthService(
            RestTemplate restTemplate,
            @Value("${integration.salesforce.login-url}") String loginUrl,
            @Value("${integration.salesforce.client-id}") String clientId,
            @Value("${integration.salesforce.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.loginUrl = loginUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getAccessToken() {
        if (cachedAccessToken != null) {
            return cachedAccessToken;
        }
        return refreshToken();
    }

    public String getInstanceUrl() {
        if (cachedInstanceUrl != null) {
            return cachedInstanceUrl;
        }
        refreshToken();
        return cachedInstanceUrl;
    }

    public String refreshToken() {
        log.info("Requesting new Salesforce OAuth token from {}", loginUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            SalesforceTokenResponse response = restTemplate.postForObject(
                    loginUrl + "/services/oauth2/token",
                    request,
                    SalesforceTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new IntegrationException("Salesforce OAuth response missing access_token");
            }

            cachedAccessToken = response.getAccessToken();
            cachedInstanceUrl = response.getInstanceUrl();
            log.info("Salesforce OAuth token obtained, instance_url={}", cachedInstanceUrl);
            return cachedAccessToken;
        } catch (RestClientException ex) {
            cachedAccessToken = null;
            cachedInstanceUrl = null;
            throw new IntegrationException("Failed to obtain Salesforce OAuth token: " + ex.getMessage(), ex);
        }
    }

    // Visible for testing
    void clearCache() {
        cachedAccessToken = null;
        cachedInstanceUrl = null;
    }
}
