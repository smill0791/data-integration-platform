package com.dataplatform.service;

import com.dataplatform.dto.ErpProductResponse;
import com.dataplatform.dto.SyncJobDTO;
import com.dataplatform.exception.IntegrationException;
import com.dataplatform.integration.ErpApiClient;
import com.dataplatform.model.RawProduct;
import com.dataplatform.model.SyncError;
import com.dataplatform.model.SyncJob;
import com.dataplatform.repository.RawProductRepository;
import com.dataplatform.repository.SyncErrorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductIntegrationServiceTest {

    @Mock private ErpApiClient erpApiClient;
    @Mock private SyncJobService syncJobService;
    @Mock private RawProductRepository rawProductRepository;
    @Mock private SyncErrorRepository syncErrorRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ProductIntegrationService productIntegrationService;

    private SyncJob runningJob;

    @BeforeEach
    void setUp() {
        runningJob = SyncJob.builder()
                .id(1L).sourceName("ERP").syncType("FULL").status("RUNNING")
                .startTime(LocalDateTime.now()).createdAt(LocalDateTime.now())
                .recordsProcessed(0).recordsFailed(0).build();
    }

    @Test
    void syncProducts_success_shouldStageAllRecords() {
        when(syncJobService.createJob("ERP", "FULL")).thenReturn(runningJob);
        List<ErpProductResponse> products = List.of(
                buildProduct("ERP-001", "Widget"),
                buildProduct("ERP-002", "Gadget")
        );
        when(erpApiClient.fetchAllProducts()).thenReturn(products);
        when(rawProductRepository.save(any(RawProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        productIntegrationService.syncProducts();

        verify(rawProductRepository, times(2)).save(any(RawProduct.class));
        verify(syncErrorRepository, never()).save(any());
    }

    @Test
    void syncProducts_partialFailure_shouldLogErrors() {
        when(syncJobService.createJob("ERP", "FULL")).thenReturn(runningJob);
        List<ErpProductResponse> products = List.of(
                buildProduct("ERP-001", "Widget"),
                buildProduct("ERP-002", "Gadget")
        );
        when(erpApiClient.fetchAllProducts()).thenReturn(products);
        when(rawProductRepository.save(any(RawProduct.class)))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new RuntimeException("DB constraint violation"));
        when(syncErrorRepository.save(any(SyncError.class))).thenAnswer(inv -> inv.getArgument(0));

        productIntegrationService.syncProducts();

        verify(syncErrorRepository).save(any(SyncError.class));
    }

    @Test
    void syncProducts_apiFails_shouldFailJob() {
        when(syncJobService.createJob("ERP", "FULL")).thenReturn(runningJob);
        when(erpApiClient.fetchAllProducts()).thenThrow(new IntegrationException("Connection refused"));
        when(syncJobService.failJob(any(), anyString())).thenAnswer(inv -> {
            runningJob.setStatus("FAILED");
            return runningJob;
        });

        SyncJobDTO result = productIntegrationService.syncProducts();

        verify(syncJobService).failJob(eq(runningJob), contains("Connection refused"));
        verify(rawProductRepository, never()).save(any());
    }

    @Test
    void syncProducts_emptyResponse_shouldCompleteWithZero() {
        when(syncJobService.createJob("ERP", "FULL")).thenReturn(runningJob);
        when(erpApiClient.fetchAllProducts()).thenReturn(Collections.emptyList());

        productIntegrationService.syncProducts();

        verify(rawProductRepository, never()).save(any());
    }

    @Test
    void syncProductsForJob_shouldUseExistingJob() {
        List<ErpProductResponse> products = List.of(buildProduct("ERP-001", "Widget"));
        when(erpApiClient.fetchAllProducts()).thenReturn(products);
        when(rawProductRepository.save(any(RawProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        productIntegrationService.syncProductsForJob(runningJob);

        verify(syncJobService, never()).createJob(any(), any());
        verify(rawProductRepository).save(any(RawProduct.class));
    }

    private ErpProductResponse buildProduct(String id, String name) {
        return ErpProductResponse.builder()
                .id(id).sku("SKU-" + id).name(name).description("Desc")
                .category("Electronics").unitPrice(29.99).quantity(50).warehouse("Warehouse-A")
                .build();
    }
}
