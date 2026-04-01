package com.dataplatform.integration;

import com.dataplatform.dto.SalesforceContact;
import com.dataplatform.dto.SalesforceQueryResult;
import com.dataplatform.exception.IntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SalesforceApiClient {

    private final RestTemplate restTemplate;
    private final SalesforceAuthService authService;
    private final String apiVersion;
    private final int maxRetries;

    private static final String SOQL_QUERY = "SELECT Id, FirstName, LastName, Email, Phone, " +
            "MailingStreet, MailingCity, MailingState, MailingPostalCode, LastModifiedDate FROM Contact";

    public SalesforceApiClient(
            RestTemplate restTemplate,
            SalesforceAuthService authService,
            @Value("${integration.salesforce.api-version}") String apiVersion,
            @Value("${integration.salesforce.max-retries}") int maxRetries) {
        this.restTemplate = restTemplate;
        this.authService = authService;
        this.apiVersion = apiVersion;
        this.maxRetries = maxRetries;
    }

    public List<SalesforceContact> fetchContacts() {
        List<SalesforceContact> allContacts = new ArrayList<>();
        String instanceUrl = authService.getInstanceUrl();
        URI uri = UriComponentsBuilder
                .fromHttpUrl(instanceUrl + "/services/data/" + apiVersion + "/query")
                .queryParam("q", SOQL_QUERY)
                .build()
                .encode()
                .toUri();

        boolean hasMore = true;
        String nextUrl = null;
        while (hasMore) {
            SalesforceQueryResult result = executeQueryWithRetry(nextUrl != null ? URI.create(nextUrl) : uri);
            if (result == null || result.getRecords() == null) {
                break;
            }
            allContacts.addAll(result.getRecords());
            log.info("Fetched {} contacts so far (totalSize={})", allContacts.size(), result.getTotalSize());

            if (result.isDone() || result.getNextRecordsUrl() == null) {
                hasMore = false;
            } else {
                nextUrl = instanceUrl + result.getNextRecordsUrl();
            }
        }

        log.info("Fetched {} total contacts from Salesforce", allContacts.size());
        return allContacts;
    }

    private SalesforceQueryResult executeQueryWithRetry(URI url) {
        boolean retried401 = false;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(authService.getAccessToken());
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<SalesforceQueryResult> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, SalesforceQueryResult.class);
                return response.getBody();

            } catch (HttpClientErrorException.Unauthorized ex) {
                if (!retried401) {
                    log.warn("Salesforce 401 — refreshing token and retrying");
                    authService.refreshToken();
                    retried401 = true;
                    continue;
                }
                throw new IntegrationException("Salesforce authentication failed after token refresh", ex);

            } catch (RestClientException ex) {
                log.warn("Salesforce API request failed (attempt={}/{}): {}",
                        attempt, maxRetries, ex.getMessage());
                if (attempt == maxRetries) {
                    throw new IntegrationException(
                            "Failed to fetch Salesforce contacts after " + maxRetries + " attempts", ex);
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IntegrationException("Interrupted during retry backoff", ie);
                }
            }
        }
        throw new IntegrationException("Failed to fetch Salesforce contacts");
    }
}
