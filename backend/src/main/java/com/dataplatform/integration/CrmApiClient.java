package com.dataplatform.integration;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.PaginatedResponse;
import com.dataplatform.exception.IntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CrmApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final int pageSize;
    private final int maxRetries;

    public CrmApiClient(
            RestTemplate restTemplate,
            @Value("${integration.crm.base-url}") String baseUrl,
            @Value("${integration.crm.page-size}") int pageSize,
            @Value("${integration.crm.max-retries}") int maxRetries) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.pageSize = pageSize;
        this.maxRetries = maxRetries;
    }

    public PaginatedResponse<CrmCustomerResponse> fetchCustomers(int page) {
        String url = String.format("%s/api/customers?page=%d&size=%d", baseUrl, page, pageSize);
        log.debug("Fetching CRM customers: {}", url);

        ResponseEntity<PaginatedResponse<CrmCustomerResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public List<CrmCustomerResponse> fetchAllCustomers() {
        List<CrmCustomerResponse> allCustomers = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            PaginatedResponse<CrmCustomerResponse> response = fetchPageWithRetry(page);
            if (response == null || response.getContent() == null) {
                break;
            }
            allCustomers.addAll(response.getContent());
            totalPages = response.getTotalPages();
            page++;
            log.info("Fetched page {}/{} ({} customers so far)", page, totalPages, allCustomers.size());
        } while (page < totalPages);

        log.info("Fetched {} total customers from CRM API", allCustomers.size());
        return allCustomers;
    }

    private PaginatedResponse<CrmCustomerResponse> fetchPageWithRetry(int page) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return fetchCustomers(page);
            } catch (RestClientException ex) {
                log.warn("CRM API request failed (page={}, attempt={}/{}): {}",
                        page, attempt, maxRetries, ex.getMessage());
                if (attempt == maxRetries) {
                    throw new IntegrationException(
                            String.format("Failed to fetch CRM customers page %d after %d attempts", page, maxRetries),
                            ex);
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IntegrationException("Interrupted during retry backoff", ie);
                }
            }
        }
        // Unreachable, but compiler needs it
        throw new IntegrationException("Failed to fetch CRM customers page " + page);
    }
}
