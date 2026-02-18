package com.dataplatform.validator;

import com.dataplatform.dto.TransformedProduct;
import com.dataplatform.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductValidationServiceTest {

    private ProductValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ProductValidationService();
    }

    @Test
    void validate_validRecord_shouldReturnValid() {
        TransformedProduct product = TransformedProduct.builder()
                .externalId("ERP-001")
                .sku("SKU001")
                .name("Widget Pro")
                .unitPrice(BigDecimal.valueOf(29.99))
                .quantity(50)
                .build();

        ValidationResult result = validationService.validate(product);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void validate_missingExternalId_shouldReturnInvalid() {
        TransformedProduct product = TransformedProduct.builder()
                .sku("SKU001")
                .name("Widget")
                .build();

        ValidationResult result = validationService.validate(product);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("external_id is required");
    }

    @Test
    void validate_missingSku_shouldReturnInvalid() {
        TransformedProduct product = TransformedProduct.builder()
                .externalId("ERP-002")
                .name("Widget")
                .build();

        ValidationResult result = validationService.validate(product);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("sku is required");
    }

    @Test
    void validate_missingName_shouldReturnInvalid() {
        TransformedProduct product = TransformedProduct.builder()
                .externalId("ERP-003")
                .sku("SKU003")
                .build();

        ValidationResult result = validationService.validate(product);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("name is required");
    }

    @Test
    void validate_negativePrice_shouldReturnInvalid() {
        TransformedProduct product = TransformedProduct.builder()
                .externalId("ERP-004")
                .sku("SKU004")
                .name("Widget")
                .unitPrice(BigDecimal.valueOf(-1.00))
                .build();

        ValidationResult result = validationService.validate(product);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("unit_price must be >= 0");
    }

    @Test
    void validate_multipleErrors_shouldReturnAll() {
        TransformedProduct product = TransformedProduct.builder()
                .unitPrice(BigDecimal.valueOf(-5.00))
                .build();

        ValidationResult result = validationService.validate(product);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(4);
        assertThat(result.getErrors()).contains("external_id is required", "sku is required",
                "name is required", "unit_price must be >= 0");
    }

    @Test
    void validate_nullPrice_shouldBeValid() {
        TransformedProduct product = TransformedProduct.builder()
                .externalId("ERP-005")
                .sku("SKU005")
                .name("Free Widget")
                .unitPrice(null)
                .build();

        ValidationResult result = validationService.validate(product);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }
}
