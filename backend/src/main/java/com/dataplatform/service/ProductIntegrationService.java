package com.dataplatform.service;

import com.dataplatform.dto.ErpProductResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.integration.ErpApiClient;
import com.dataplatform.model.RawProduct;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawProductRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIntegrationService {

    private final ErpApiClient erpApiClient;
    private final SyncJobService syncJobService;
    private final RawProductRepository rawProductRepository;
    private final SyncErrorRepository syncErrorRepository;
    private final ObjectMapper objectMapper;

    public SyncJobDTO syncProducts() {
        SyncJob job = syncJobService.createJob("ERP", "FULL");
        return syncProductsForJob(job);
    }

    public SyncJobDTO syncProductsForJob(SyncJob job) {
        List<ErpProductResponse> products;
        try {
            products = erpApiClient.fetchAllProducts();
        } catch (Exception ex) {
            log.error("Failed to fetch products from ERP API", ex);
            syncJobService.failJob(job, "ERP API fetch failed: " + ex.getMessage());
            return SyncJobDTO.fromEntity(job);
        }

        int processed = 0;
        int failed = 0;

        for (ErpProductResponse product : products) {
            try {
                String rawJson = objectMapper.writeValueAsString(product);
                RawProduct rawProduct = RawProduct.builder()
                        .syncJob(job)
                        .externalId(product.getId())
                        .rawData(rawJson)
                        .build();
                rawProductRepository.save(rawProduct);
                processed++;
            } catch (Exception ex) {
                failed++;
                log.warn("Failed to stage product {}: {}", product.getId(), ex.getMessage());
                try {
                    SyncError error = SyncError.builder()
                            .syncJob(job)
                            .errorType("STAGING_ERROR")
                            .errorMessage(ex.getMessage())
                            .failedRecord(product.getId())
                            .build();
                    syncErrorRepository.save(error);
                } catch (Exception errorEx) {
                    log.error("Failed to log sync error for product {}", product.getId(), errorEx);
                }
            }
        }

        log.info("Product staging completed for job {}: processed={}, failed={}", job.getId(), processed, failed);
        return SyncJobDTO.fromEntity(job);
    }
}
