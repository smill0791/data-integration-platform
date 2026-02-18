package com.dataplatform.validator;

import com.dataplatform.dto.TransformedInvoice;
import com.dataplatform.dto.ValidationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class InvoiceValidationService {

    private static final Set<String> VALID_STATUSES = Set.of("paid", "pending", "overdue");

    public ValidationResult validate(TransformedInvoice invoice) {
        List<String> errors = new ArrayList<>();

        if (invoice.getExternalId() == null || invoice.getExternalId().isBlank()) {
            errors.add("external_id is required");
        }

        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            errors.add("invoice_number is required");
        }

        if (invoice.getCustomerName() == null || invoice.getCustomerName().isBlank()) {
            errors.add("customer_name is required");
        }

        if (invoice.getAmount() != null && invoice.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("amount must be >= 0");
        }

        if (invoice.getCurrency() == null || invoice.getCurrency().isBlank()) {
            errors.add("currency is required");
        }

        if (invoice.getStatus() != null && !invoice.getStatus().isBlank()
                && !VALID_STATUSES.contains(invoice.getStatus())) {
            errors.add("status must be one of: paid, pending, overdue");
        }

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }
}
