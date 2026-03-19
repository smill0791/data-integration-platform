package com.dataplatform.service;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.SalesforceContact;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.integration.SalesforceApiClient;
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
public class SalesforceIntegrationService {

    private final SalesforceApiClient salesforceApiClient;
    private final RawCustomerRepository rawCustomerRepository;
    private final SyncErrorRepository syncErrorRepository;
    private final SyncJobService syncJobService;
    private final ObjectMapper objectMapper;

    public SyncJobDTO syncContactsForJob(SyncJob job) {
        List<SalesforceContact> contacts;
        try {
            contacts = salesforceApiClient.fetchContacts();
        } catch (Exception ex) {
            log.error("Failed to fetch contacts from Salesforce API", ex);
            syncJobService.failJob(job, "Salesforce API fetch failed: " + ex.getMessage());
            return SyncJobDTO.fromEntity(job);
        }

        int processed = 0;
        int failed = 0;

        for (SalesforceContact contact : contacts) {
            try {
                CrmCustomerResponse normalized = normalizeToCrmFormat(contact);
                String rawJson = objectMapper.writeValueAsString(normalized);
                RawCustomer rawCustomer = RawCustomer.builder()
                        .syncJob(job)
                        .externalId(contact.getId())
                        .rawData(rawJson)
                        .build();
                rawCustomerRepository.save(rawCustomer);
                processed++;
            } catch (Exception ex) {
                failed++;
                log.warn("Failed to stage Salesforce contact {}: {}", contact.getId(), ex.getMessage());
                try {
                    SyncError error = SyncError.builder()
                            .syncJob(job)
                            .errorType("STAGING_ERROR")
                            .errorMessage(ex.getMessage())
                            .failedRecord(contact.getId())
                            .build();
                    syncErrorRepository.save(error);
                } catch (Exception errorEx) {
                    log.error("Failed to log sync error for contact {}", contact.getId(), errorEx);
                }
            }
        }

        log.info("Salesforce staging completed for job {}: processed={}, failed={}", job.getId(), processed, failed);
        return SyncJobDTO.fromEntity(job);
    }

    CrmCustomerResponse normalizeToCrmFormat(SalesforceContact contact) {
        String name = buildName(contact.getFirstName(), contact.getLastName());

        CrmCustomerResponse.Address address = CrmCustomerResponse.Address.builder()
                .street(contact.getMailingStreet())
                .city(contact.getMailingCity())
                .state(contact.getMailingState())
                .zipCode(contact.getMailingPostalCode())
                .build();

        return CrmCustomerResponse.builder()
                .id(contact.getId())
                .name(name)
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .address(address)
                .lastUpdated(contact.getLastModifiedDate())
                .build();
    }

    private String buildName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}
