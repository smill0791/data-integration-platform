package com.dataplatform.integration;

import com.dataplatform.dto.ErpProductResponse;
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
public class ErpApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final int pageSize;
    private final int maxRetries;

    public ErpApiClient(
            RestTemplate restTemplate,
            @Value("${integration.erp.base-url}") String baseUrl,
            @Value("${integration.erp.page-size}") int pageSize,
            @Value("${integration.erp.max-retries}") int maxRetries) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.pageSize = pageSize;
        this.maxRetries = maxRetries;
    }

    public PaginatedResponse<ErpProductResponse> fetchProducts(int page) {
        String url = String.format("%s/api/products?page=%d&size=%d", baseUrl, page, pageSize);
        log.debug("Fetching ERP products: {}", url);

        ResponseEntity<PaginatedResponse<ErpProductResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    public List<ErpProductResponse> fetchAllProducts() {
        List<ErpProductResponse> allProducts = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            PaginatedResponse<ErpProductResponse> response = fetchPageWithRetry(page);
            if (response == null || response.getContent() == null) {
                break;
            }
            allProducts.addAll(response.getContent());
            totalPages = response.getTotalPages();
            page++;
            log.info("Fetched page {}/{} ({} products so far)", page, totalPages, allProducts.size());
        } while (page < totalPages);

        log.info("Fetched {} total products from ERP API", allProducts.size());
        return allProducts;
    }

    private PaginatedResponse<ErpProductResponse> fetchPageWithRetry(int page) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return fetchProducts(page);
            } catch (RestClientException ex) {
                log.warn("ERP API request failed (page={}, attempt={}/{}): {}",
                        page, attempt, maxRetries, ex.getMessage());
                if (attempt == maxRetries) {
                    throw new IntegrationException(
                            String.format("Failed to fetch ERP products page %d after %d attempts", page, maxRetries),
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
        throw new IntegrationException("Failed to fetch ERP products page " + page);
    }
}
