package com.dataplatform.service;

import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.dto.TransformedCustomer;
import com.dataplatform.dto.ValidationResult;
import com.dataplatform.model.RawCustomer;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawCustomerRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.transformer.CustomerTransformationService;
import com.dataplatform.validator.CustomerValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerPipelineService {

    private final CustomerIntegrationService customerIntegrationService;
    private final CustomerTransformationService transformationService;
    private final CustomerValidationService validationService;
    private final CustomerLoadService loadService;
    private final SyncJobService syncJobService;
    private final RawCustomerRepository rawCustomerRepository;
    private final SyncErrorRepository syncErrorRepository;

    public SyncJobDTO runFullPipeline() {
        SyncJobDTO stagingResult = customerIntegrationService.syncCustomers();

        if ("FAILED".equals(stagingResult.getStatus())) {
            log.warn("Staging failed for job {}, skipping pipeline", stagingResult.getId());
            return stagingResult;
        }

        return runPipelineForJob(stagingResult.getId());
    }

    public SyncJobDTO runPipelineForJob(Long jobId) {
        SyncJob job = syncJobService.getJobEntity(jobId);

        // If job doesn't have staging data yet, run staging first
        List<RawCustomer> rawCustomers = rawCustomerRepository.findBySyncJobId(job.getId());
        if (rawCustomers.isEmpty() && "RUNNING".equals(job.getStatus())) {
            SyncJobDTO stagingResult = customerIntegrationService.syncCustomersForJob(job);
            if ("FAILED".equals(stagingResult.getStatus())) {
                log.warn("Staging failed for job {}, skipping pipeline", jobId);
                return stagingResult;
            }
            rawCustomers = rawCustomerRepository.findBySyncJobId(job.getId());
        }

        int loaded = 0;
        int failed = 0;

        for (RawCustomer raw : rawCustomers) {
            try {
                TransformedCustomer transformed = transformationService.transform(raw.getRawData());

                ValidationResult validationResult = validationService.validate(transformed);
                if (!validationResult.isValid()) {
                    failed++;
                    log.warn("Validation failed for customer {}: {}", raw.getExternalId(), validationResult.getErrors());
                    SyncError error = SyncError.builder()
                            .syncJob(job)
                            .errorType("VALIDATION_ERROR")
                            .errorMessage(String.join("; ", validationResult.getErrors()))
                            .failedRecord(raw.getExternalId())
                            .build();
                    syncErrorRepository.save(error);
                    continue;
                }

                loadService.loadCustomer(transformed);
                loaded++;
            } catch (Exception ex) {
                failed++;
                log.error("Pipeline error for customer {}: {}", raw.getExternalId(), ex.getMessage());
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
