package com.dataplatform.service;

import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.dto.TransformedInvoice;
import com.dataplatform.dto.ValidationResult;
import com.dataplatform.model.RawInvoice;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawInvoiceRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.dataplatform.transformer.InvoiceTransformationService;
import com.dataplatform.validator.InvoiceValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePipelineService {

    private final InvoiceIntegrationService invoiceIntegrationService;
    private final InvoiceTransformationService transformationService;
    private final InvoiceValidationService validationService;
    private final InvoiceLoadService loadService;
    private final SyncJobService syncJobService;
    private final RawInvoiceRepository rawInvoiceRepository;
    private final SyncErrorRepository syncErrorRepository;

    public SyncJobDTO runFullPipeline() {
        SyncJobDTO stagingResult = invoiceIntegrationService.syncInvoices();

        if ("FAILED".equals(stagingResult.getStatus())) {
            log.warn("Staging failed for job {}, skipping pipeline", stagingResult.getId());
            return stagingResult;
        }

        return runPipelineForJob(stagingResult.getId());
    }

    public SyncJobDTO runPipelineForJob(Long jobId) {
        SyncJob job = syncJobService.getJobEntity(jobId);

        List<RawInvoice> rawInvoices = rawInvoiceRepository.findBySyncJobId(job.getId());
        if (rawInvoices.isEmpty() && "RUNNING".equals(job.getStatus())) {
            SyncJobDTO stagingResult = invoiceIntegrationService.syncInvoicesForJob(job);
            if ("FAILED".equals(stagingResult.getStatus())) {
                log.warn("Staging failed for job {}, skipping pipeline", jobId);
                return stagingResult;
            }
            rawInvoices = rawInvoiceRepository.findBySyncJobId(job.getId());
        }

        int loaded = 0;
        int failed = 0;

        for (RawInvoice raw : rawInvoices) {
            try {
                TransformedInvoice transformed = transformationService.transform(raw.getRawData());

                ValidationResult validationResult = validationService.validate(transformed);
                if (!validationResult.isValid()) {
                    failed++;
                    log.warn("Validation failed for invoice {}: {}", raw.getExternalId(), validationResult.getErrors());
                    SyncError error = SyncError.builder()
                            .syncJob(job)
                            .errorType("VALIDATION_ERROR")
                            .errorMessage(String.join("; ", validationResult.getErrors()))
                            .failedRecord(raw.getExternalId())
                            .build();
                    syncErrorRepository.save(error);
                    continue;
                }

                loadService.loadInvoice(transformed);
                loaded++;
            } catch (Exception ex) {
                failed++;
                log.error("Pipeline error for invoice {}: {}", raw.getExternalId(), ex.getMessage());
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
