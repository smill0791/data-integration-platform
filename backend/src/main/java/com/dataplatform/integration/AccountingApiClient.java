package com.dataplatform.integration;

import com.dataplatform.dto.AccountingInvoiceResponse;
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
public class AccountingApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final int pageSize;
    private final int maxRetries;

    public AccountingApiClient(
            RestTemplate restTemplate,
            @Value("${integration.accounting.base-url}") String baseUrl,
            @Value("${integration.accounting.page-size}") int pageSize,
            @Value("${integration.accounting.max-retries}") int maxRetries) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.pageSize = pageSize;
        this.maxRetries = maxRetries;
    }

    public PaginatedResponse<AccountingInvoiceResponse> fetchInvoices(int page) {
        String url = String.format("%s/api/invoices?page=%d&size=%d", baseUrl, page, pageSize);
        log.debug("Fetching Accounting invoices: {}", url);

        ResponseEntity<PaginatedResponse<AccountingInvoiceResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public List<AccountingInvoiceResponse> fetchAllInvoices() {
        List<AccountingInvoiceResponse> allInvoices = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            PaginatedResponse<AccountingInvoiceResponse> response = fetchPageWithRetry(page);
            if (response == null || response.getContent() == null) {
                break;
            }
            allInvoices.addAll(response.getContent());
            totalPages = response.getTotalPages();
            page++;
            log.info("Fetched page {}/{} ({} invoices so far)", page, totalPages, allInvoices.size());
        } while (page < totalPages);

        log.info("Fetched {} total invoices from Accounting API", allInvoices.size());
        return allInvoices;
    }

    private PaginatedResponse<AccountingInvoiceResponse> fetchPageWithRetry(int page) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return fetchInvoices(page);
            } catch (RestClientException ex) {
                log.warn("Accounting API request failed (page={}, attempt={}/{}): {}",
                        page, attempt, maxRetries, ex.getMessage());
                if (attempt == maxRetries) {
                    throw new IntegrationException(
                            String.format("Failed to fetch Accounting invoices page %d after %d attempts", page, maxRetries),
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
        throw new IntegrationException("Failed to fetch Accounting invoices page " + page);
    }
}
