package com.dataplatform.service;

import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.dto.TransformedProduct;
import com.dataplatform.dto.ValidationResult;
import com.dataplatform.model.RawProduct;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawProductRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.transformer.ProductTransformationService;
import com.dataplatform.validator.ProductValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPipelineService {

    private final ProductIntegrationService productIntegrationService;
    private final ProductTransformationService transformationService;
    private final ProductValidationService validationService;
    private final ProductLoadService loadService;
    private final SyncJobService syncJobService;
    private final RawProductRepository rawProductRepository;
    private final SyncErrorRepository syncErrorRepository;

    public SyncJobDTO runFullPipeline() {
        SyncJobDTO stagingResult = productIntegrationService.syncProducts();

        if ("FAILED".equals(stagingResult.getStatus())) {
            log.warn("Staging failed for job {}, skipping pipeline", stagingResult.getId());
            return stagingResult;
        }

        return runPipelineForJob(stagingResult.getId());
    }

    public SyncJobDTO runPipelineForJob(Long jobId) {
        SyncJob job = syncJobService.getJobEntity(jobId);

        List<RawProduct> rawProducts = rawProductRepository.findBySyncJobId(job.getId());
        if (rawProducts.isEmpty() && "RUNNING".equals(job.getStatus())) {
            SyncJobDTO stagingResult = productIntegrationService.syncProductsForJob(job);
            if ("FAILED".equals(stagingResult.getStatus())) {
                log.warn("Staging failed for job {}, skipping pipeline", jobId);
                return stagingResult;
            }
            rawProducts = rawProductRepository.findBySyncJobId(job.getId());
        }

        int loaded = 0;
        int failed = 0;

        for (RawProduct raw : rawProducts) {
            try {
                TransformedProduct transformed = transformationService.transform(raw.getRawData());

                ValidationResult validationResult = validationService.validate(transformed);
                if (!validationResult.isValid()) {
                    failed++;
                    log.warn("Validation failed for product {}: {}", raw.getExternalId(), validationResult.getErrors());
                    SyncError error = SyncError.builder()
                            .syncJob(job)
                            .errorType("VALIDATION_ERROR")
                            .errorMessage(String.join("; ", validationResult.getErrors()))
                            .failedRecord(raw.getExternalId())
                            .build();
                    syncErrorRepository.save(error);
                    continue;
                }

                loadService.loadProduct(transformed);
                loaded++;
            } catch (Exception ex) {
                failed++;
                log.error("Pipeline error for product {}: {}", raw.getExternalId(), ex.getMessage());
                SyncError error = SyncError.builder()
                        .syncJob(job)
                        .errorType("PIPELINE_ERROR")
                        .errorMessage(ex.getMessage())
                        .failedRecord(raw.getExternalId())
                        .build();
                syncErrorRepository.save(error);
            }
        }

        syncJobService.completeJob(job, loaded, failed);
        log.info("Pipeline completed for job {}: loaded={}, failed={}", jobId, loaded, failed);
        return SyncJobDTO.fromEntity(job);
    }
}
