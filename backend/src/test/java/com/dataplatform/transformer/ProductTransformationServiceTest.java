package com.dataplatform.transformer;

import com.dataplatform.dto.ErpProductResponse;
import com.dataplatform.dto.TransformedProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTransformationServiceTest {

    private ProductTransformationService transformationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        transformationService = new ProductTransformationService(objectMapper);
    }

    @Test
    void transform_happyPath_shouldNormalizeAllFields() throws Exception {
        String json = objectMapper.writeValueAsString(buildProduct("ERP-001", "  abc123  ",
                "  Widget Pro  ", "A great widget", "Electronics", 29.99, 50, "Warehouse-A"));

        TransformedProduct result = transformationService.transform(json);

        assertThat(result.getExternalId()).isEqualTo("ERP-001");
        assertThat(result.getSku()).isEqualTo("ABC123");
        assertThat(result.getName()).isEqualTo("Widget Pro");
        assertThat(result.getDescription()).isEqualTo("A great widget");
        assertThat(result.getCategory()).isEqualTo("Electronics");
        assertThat(result.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(29.99));
        assertThat(result.getQuantity()).isEqualTo(50);
        assertThat(result.getWarehouse()).isEqualTo("Warehouse-A");
        assertThat(result.getRawData()).isNotNull();
    }

    @Test
    void transform_nullDescription_shouldReturnNull() throws Exception {
        String json = objectMapper.writeValueAsString(buildProduct("ERP-002", "SKU002",
                "Gadget", null, "Hardware", 10.0, 100, "Warehouse-B"));

        TransformedProduct result = transformationService.transform(json);

        assertThat(result.getDescription()).isNull();
        assertThat(result.getName()).isEqualTo("Gadget");
    }

    @Test
    void transform_nullCategory_shouldReturnNull() throws Exception {
        String json = objectMapper.writeValueAsString(buildProduct("ERP-003", "SKU003",
                "Thing", "A thing", null, 5.0, 10, "Warehouse-C"));

        TransformedProduct result = transformationService.transform(json);

        assertThat(result.getCategory()).isNull();
    }

    @Test
    void transform_skuShouldBeUppercased() throws Exception {
        String json = objectMapper.writeValueAsString(buildProduct("ERP-004", "  lowSku  ",
                "Item", "Desc", "Office Supplies", 1.99, 200, "Warehouse-A"));

        TransformedProduct result = transformationService.transform(json);

        assertThat(result.getSku()).isEqualTo("LOWSKU");
    }

    @Test
    void transform_negativeQuantity_shouldClampToZero() throws Exception {
        String json = objectMapper.writeValueAsString(buildProduct("ERP-005", "SKU005",
                "Negative Stock Item", "Desc", "Electronics", 15.0, -5, "Warehouse-B"));

        TransformedProduct result = transformationService.transform(json);

        assertThat(result.getQuantity()).isEqualTo(0);
    }

    @Test
    void transform_invalidJson_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> transformationService.transform("not valid json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse raw product JSON");
    }

    @Test
    void transform_zeroPrice_shouldBePreserved() throws Exception {
        String json = objectMapper.writeValueAsString(buildProduct("ERP-006", "FREE001",
                "Free Sample", "Complimentary", "Software", 0.0, 1000, "Warehouse-D"));

        TransformedProduct result = transformationService.transform(json);

        assertThat(result.getUnitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void transform_nullWarehouse_shouldReturnNull() throws Exception {
        String json = objectMapper.writeValueAsString(buildProduct("ERP-007", "SKU007",
                "Unassigned Item", "Desc", "Hardware", 25.0, 10, null));

        TransformedProduct result = transformationService.transform(json);

        assertThat(result.getWarehouse()).isNull();
    }

    private ErpProductResponse buildProduct(String id, String sku, String name, String description,
                                             String category, double unitPrice, int quantity, String warehouse) {
        return ErpProductResponse.builder()
                .id(id)
                .sku(sku)
                .name(name)
                .description(description)
                .category(category)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .warehouse(warehouse)
                .build();
    }
}
