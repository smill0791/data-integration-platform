package com.dataplatform.service;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.integration.CrmApiClient;
import com.dataplatform.model.RawCustomer;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawCustomerRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerIntegrationService {

    private final CrmApiClient crmApiClient;
    private final SyncJobService syncJobService;
    private final RawCustomerRepository rawCustomerRepository;
    private final SyncErrorRepository syncErrorRepository;
    private final ObjectMapper objectMapper;

    public SyncJobDTO syncCustomers() {
        SyncJob job = syncJobService.createJob("CRM", "FULL");

        List<CrmCustomerResponse> customers;
        try {
            customers = crmApiClient.fetchAllCustomers();
        } catch (Exception ex) {
            log.error("Failed to fetch customers from CRM API", ex);
            syncJobService.failJob(job, "CRM API fetch failed: " + ex.getMessage());
            return SyncJobDTO.fromEntity(job);
        }

        int processed = 0;
        int failed = 0;

        for (CrmCustomerResponse customer : customers) {
            try {
                String rawJson = objectMapper.writeValueAsString(customer);
                RawCustomer rawCustomer = RawCustomer.builder()
                        .syncJob(job)
                        .externalId(customer.getId())
                        .rawData(rawJson)
                        .build();
                rawCustomerRepository.save(rawCustomer);
                processed++;
            } catch (Exception ex) {
                failed++;
                log.warn("Failed to stage customer {}: {}", customer.getId(), ex.getMessage());
                try {
                    SyncError error = SyncError.builder()
                            .syncJob(job)
                            .errorType("STAGING_ERROR")
                            .errorMessage(ex.getMessage())
                            .failedRecord(customer.getId())
                            .build();
                    syncErrorRepository.save(error);
                } catch (Exception errorEx) {
                    log.error("Failed to log sync error for customer {}", customer.getId(), errorEx);
                }
            }
        }

        syncJobService.completeJob(job, processed, failed);
        log.info("Customer sync completed: processed={}, failed={}", processed, failed);
        return SyncJobDTO.fromEntity(job);
    }
}
