package com.dataplatform.validator;

import com.dataplatform.dto.TransformedInvoice;
import com.dataplatform.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceValidationServiceTest {

    private InvoiceValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new InvoiceValidationService();
    }

    @Test
    void validate_validRecord_shouldReturnValid() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .externalId("ACC-001").invoiceNumber("INV-001").customerName("Acme Corp")
                .amount(BigDecimal.valueOf(1500.50)).currency("USD").status("paid")
                .dueDate(LocalDate.of(2025, 6, 15)).build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void validate_missingExternalId_shouldReturnInvalid() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .invoiceNumber("INV-001").customerName("Acme").currency("USD").build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("external_id is required");
    }

    @Test
    void validate_missingInvoiceNumber_shouldReturnInvalid() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .externalId("ACC-002").customerName("Acme").currency("USD").build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("invoice_number is required");
    }

    @Test
    void validate_missingCustomerName_shouldReturnInvalid() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .externalId("ACC-003").invoiceNumber("INV-003").currency("USD").build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("customer_name is required");
    }

    @Test
    void validate_negativeAmount_shouldReturnInvalid() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .externalId("ACC-004").invoiceNumber("INV-004").customerName("Acme")
                .amount(BigDecimal.valueOf(-100.0)).currency("USD").status("paid").build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("amount must be >= 0");
    }

    @Test
    void validate_missingCurrency_shouldReturnInvalid() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .externalId("ACC-005").invoiceNumber("INV-005").customerName("Acme")
                .amount(BigDecimal.valueOf(100.0)).build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("currency is required");
    }

    @Test
    void validate_invalidStatus_shouldReturnInvalid() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .externalId("ACC-006").invoiceNumber("INV-006").customerName("Acme")
                .amount(BigDecimal.valueOf(100.0)).currency("USD").status("cancelled").build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("status must be one of: paid, pending, overdue");
    }

    @Test
    void validate_multipleErrors_shouldReturnAll() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .amount(BigDecimal.valueOf(-50.0)).status("invalid").build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(6);
    }

    @Test
    void validate_nullStatus_shouldBeValid() {
        TransformedInvoice invoice = TransformedInvoice.builder()
                .externalId("ACC-007").invoiceNumber("INV-007").customerName("Acme")
                .amount(BigDecimal.valueOf(100.0)).currency("USD").status(null).build();

        ValidationResult result = validationService.validate(invoice);

        assertThat(result.isValid()).isTrue();
    }
}
