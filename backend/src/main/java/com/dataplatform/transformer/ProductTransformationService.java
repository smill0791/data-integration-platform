package com.dataplatform.transformer;

import com.dataplatform.dto.ErpProductResponse;
import com.dataplatform.dto.TransformedProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransformationService {

    private final ObjectMapper objectMapper;

    public TransformedProduct transform(String rawJson) {
        ErpProductResponse erp;
        try {
            erp = objectMapper.readValue(rawJson, ErpProductResponse.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse raw product JSON: " + ex.getMessage(), ex);
        }

        return TransformedProduct.builder()
                .externalId(erp.getId())
                .sku(normalizeSku(erp.getSku()))
                .name(normalizeString(erp.getName()))
                .description(normalizeString(erp.getDescription()))
                .category(normalizeString(erp.getCategory()))
                .unitPrice(BigDecimal.valueOf(erp.getUnitPrice()))
                .quantity(clampQuantity(erp.getQuantity()))
                .warehouse(normalizeString(erp.getWarehouse()))
                .rawData(rawJson)
                .build();
    }

    private String normalizeString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.isBlank()) {
            return null;
        }
        return sku.trim().toUpperCase();
    }

    private Integer clampQuantity(int quantity) {
        return Math.max(0, quantity);
    }
}
