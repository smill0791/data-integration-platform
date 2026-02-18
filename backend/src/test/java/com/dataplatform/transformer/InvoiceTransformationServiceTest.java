package com.dataplatform.transformer;

import com.dataplatform.dto.AccountingInvoiceResponse;
import com.dataplatform.dto.TransformedInvoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTransformationServiceTest {

    private InvoiceTransformationService transformationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        transformationService = new InvoiceTransformationService(objectMapper);
    }

    @Test
    void transform_happyPath_shouldNormalizeAllFields() throws Exception {
        String json = objectMapper.writeValueAsString(buildInvoice("ACC-001", "INV-001",
                "  Acme Corp  ", 1500.50, "  usd  ", "  Paid  ", "2025-06-15"));

        TransformedInvoice result = transformationService.transform(json);

        assertThat(result.getExternalId()).isEqualTo("ACC-001");
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-001");
        assertThat(result.getCustomerName()).isEqualTo("Acme Corp");
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500.50));
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getStatus()).isEqualTo("paid");
        assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2025, 6, 15));
        assertThat(result.getRawData()).isNotNull();
    }

    @Test
    void transform_nullCustomerName_shouldReturnNull() throws Exception {
        String json = objectMapper.writeValueAsString(buildInvoice("ACC-002", "INV-002",
                null, 100.0, "EUR", "pending", "2025-07-01"));

        TransformedInvoice result = transformationService.transform(json);

        assertThat(result.getCustomerName()).isNull();
    }

    @Test
    void transform_nullCurrency_shouldReturnNull() throws Exception {
        String json = objectMapper.writeValueAsString(buildInvoice("ACC-003", "INV-003",
                "Corp Inc", 200.0, null, "overdue", "2025-08-01"));

        TransformedInvoice result = transformationService.transform(json);

        assertThat(result.getCurrency()).isNull();
    }

    @Test
    void transform_statusShouldBeLowercased() throws Exception {
        String json = objectMapper.writeValueAsString(buildInvoice("ACC-004", "INV-004",
                "Big Co", 300.0, "GBP", "  OVERDUE  ", "2025-09-01"));

        TransformedInvoice result = transformationService.transform(json);

        assertThat(result.getStatus()).isEqualTo("overdue");
    }

    @Test
    void transform_currencyShouldBeUppercased() throws Exception {
        String json = objectMapper.writeValueAsString(buildInvoice("ACC-005", "INV-005",
                "Small Co", 50.0, "  eur  ", "paid", "2025-10-01"));

        TransformedInvoice result = transformationService.transform(json);

        assertThat(result.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void transform_invalidDueDate_shouldReturnNull() throws Exception {
        String json = objectMapper.writeValueAsString(buildInvoice("ACC-006", "INV-006",
                "Test Co", 75.0, "USD", "pending", "not-a-date"));

        TransformedInvoice result = transformationService.transform(json);

        assertThat(result.getDueDate()).isNull();
    }

    @Test
    void transform_invalidJson_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> transformationService.transform("not valid json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse raw invoice JSON");
    }

    @Test
    void transform_nullDueDate_shouldReturnNull() throws Exception {
        String json = objectMapper.writeValueAsString(buildInvoice("ACC-007", "INV-007",
                "No Date Co", 100.0, "CAD", "paid", null));

        TransformedInvoice result = transformationService.transform(json);

        assertThat(result.getDueDate()).isNull();
    }

    private AccountingInvoiceResponse buildInvoice(String id, String invoiceNumber, String customerName,
                                                    double amount, String currency, String status, String dueDate) {
        return AccountingInvoiceResponse.builder()
                .id(id)
                .invoiceNumber(invoiceNumber)
                .customerName(customerName)
                .amount(amount)
                .currency(currency)
                .status(status)
                .dueDate(dueDate)
                .lineItems(List.of(Map.of("description", "Item 1", "quantity", 1, "unitPrice", amount)))
                .build();
    }
}
