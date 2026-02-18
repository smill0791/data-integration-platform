package com.dataplatform.service;

import com.dataplatform.dto.TransformedProduct;
import com.dataplatform.model.ValidatedProduct;
import com.dataplatform.repository.ValidatedProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductLoadServiceTest {

    @Mock
    private ValidatedProductRepository validatedProductRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ProductLoadService productLoadService;

    private TransformedProduct sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = TransformedProduct.builder()
                .externalId("ERP-001")
                .sku("SKU001")
                .name("Widget Pro")
                .description("A great widget")
                .category("Electronics")
                .unitPrice(BigDecimal.valueOf(29.99))
                .quantity(50)
                .warehouse("Warehouse-A")
                .build();
    }

    @Test
    void loadProduct_newRecord_shouldSaveAndUpsert() {
        when(validatedProductRepository.findByExternalId("ERP-001")).thenReturn(Optional.empty());
        when(validatedProductRepository.save(any(ValidatedProduct.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        productLoadService.loadProduct(sampleProduct);

        verify(validatedProductRepository).save(argThat(vp ->
                "ERP-001".equals(vp.getExternalId()) && "Widget Pro".equals(vp.getName())));
        verify(jdbcTemplate).update(eq("EXEC [final].upsert_products ?, ?, ?, ?, ?, ?, ?, ?, ?"),
                eq("ERP-001"), eq("SKU001"), eq("Widget Pro"), eq("A great widget"),
                eq("Electronics"), eq(BigDecimal.valueOf(29.99)), eq(50), eq("Warehouse-A"), eq("ERP"));
    }

    @Test
    void loadProduct_existingRecord_shouldUpdateAndUpsert() {
        ValidatedProduct existing = ValidatedProduct.builder()
                .id(1L)
                .externalId("ERP-001")
                .sku("OLDSKU")
                .name("Old Name")
                .build();
        when(validatedProductRepository.findByExternalId("ERP-001")).thenReturn(Optional.of(existing));
        when(validatedProductRepository.save(any(ValidatedProduct.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        productLoadService.loadProduct(sampleProduct);

        verify(validatedProductRepository).save(argThat(vp ->
                vp.getId() != null && "Widget Pro".equals(vp.getName()) && "SKU001".equals(vp.getSku())));
    }

    @Test
    void loadProduct_jdbcFailure_shouldPropagate() {
        when(validatedProductRepository.findByExternalId("ERP-001")).thenReturn(Optional.empty());
        when(validatedProductRepository.save(any(ValidatedProduct.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> productLoadService.loadProduct(sampleProduct))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB connection lost");
    }
}
