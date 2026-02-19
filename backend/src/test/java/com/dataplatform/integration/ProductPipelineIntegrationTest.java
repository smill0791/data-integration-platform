package com.dataplatform.integration;

import com.dataplatform.dto.ErpProductResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.*;
import com.dataplatform.service.ProductPipelineService;
import com.dataplatform.service.SyncJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPipelineIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductPipelineService productPipelineService;

    @Autowired
    private SyncJobService syncJobService;

    @Autowired
    private RawProductRepository rawProductRepository;

    @Autowired
    private ValidatedProductRepository validatedProductRepository;

    @Autowired
    private FinalProductRepository finalProductRepository;

    @Autowired
    private SyncErrorRepository syncErrorRepository;

    @Test
    void fullProductPipeline_shouldPopulateAllSchemas() {
        List<ErpProductResponse> products = List.of(
                WireMockStubs.createProduct("P001", "sku-abc", "Widget A", 29.99, 100),
                WireMockStubs.createProduct("P002", "sku-def", "Widget B", 49.99, 50),
                WireMockStubs.createProduct("P003", "sku-ghi", "Widget C", 9.99, 200)
        );
        WireMockStubs.stubProducts(wireMockServer, products);

        SyncJob job = syncJobService.createJob("ERP", "FULL");
        SyncJobDTO result = productPipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(rawProductRepository.findBySyncJobId(job.getId())).hasSize(3);
        assertThat(validatedProductRepository.findAll()).hasSize(3);
        assertThat(finalProductRepository.findAll()).hasSize(3);

        // SKU should be uppercased by transformation
        var finalProduct = finalProductRepository.findByExternalId("P001");
        assertThat(finalProduct).isPresent();
        assertThat(finalProduct.get().getSku()).isEqualTo("SKU-ABC");
    }

    @Test
    void productUpsert_secondSyncUpdates() {
        List<ErpProductResponse> products1 = List.of(
                WireMockStubs.createProduct("P001", "sku-abc", "Widget A", 29.99, 100),
                WireMockStubs.createProduct("P002", "sku-def", "Widget B", 49.99, 50)
        );
        WireMockStubs.stubProducts(wireMockServer, products1);

        SyncJob job1 = syncJobService.createJob("ERP", "FULL");
        productPipelineService.runPipelineForJob(job1.getId());

        // Second sync with updated price/quantity
        wireMockServer.resetAll();
        List<ErpProductResponse> products2 = List.of(
                WireMockStubs.createProduct("P001", "sku-abc", "Widget A", 39.99, 150),
                WireMockStubs.createProduct("P002", "sku-def", "Widget B", 59.99, 75)
        );
        WireMockStubs.stubProducts(wireMockServer, products2);

        SyncJob job2 = syncJobService.createJob("ERP", "FULL");
        productPipelineService.runPipelineForJob(job2.getId());

        // MERGE should update, not duplicate
        assertThat(finalProductRepository.findAll()).hasSize(2);

        var updated = finalProductRepository.findByExternalId("P001");
        assertThat(updated).isPresent();
        assertThat(updated.get().getQuantity()).isEqualTo(150);
    }

    @Test
    void productPipeline_invalidPrice_failsValidation() {
        List<ErpProductResponse> products = List.of(
                WireMockStubs.createProduct("P001", "sku-abc", "Widget A", 29.99, 100),
                WireMockStubs.createProduct("P002", "sku-def", "Widget B", -5.00, 50)  // negative price
        );
        WireMockStubs.stubProducts(wireMockServer, products);

        SyncJob job = syncJobService.createJob("ERP", "FULL");
        SyncJobDTO result = productPipelineService.runPipelineForJob(job.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRecordsProcessed()).isEqualTo(1);
        assertThat(result.getRecordsFailed()).isEqualTo(1);
        assertThat(finalProductRepository.findAll()).hasSize(1);

        var errors = syncErrorRepository.findBySyncJobIdOrderByOccurredAtDesc(job.getId());
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getErrorType()).isEqualTo("VALIDATION_ERROR");
    }
}
