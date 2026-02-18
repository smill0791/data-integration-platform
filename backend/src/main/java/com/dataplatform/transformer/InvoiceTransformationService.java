package com.dataplatform.transformer;

import com.dataplatform.dto.AccountingInvoiceResponse;
import com.dataplatform.dto.TransformedInvoice;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceTransformationService {

    private final ObjectMapper objectMapper;

    public TransformedInvoice transform(String rawJson) {
        AccountingInvoiceResponse invoice;
        try {
            invoice = objectMapper.readValue(rawJson, AccountingInvoiceResponse.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse raw invoice JSON: " + ex.getMessage(), ex);
        }

        return TransformedInvoice.builder()
                .externalId(invoice.getId())
                .invoiceNumber(normalizeString(invoice.getInvoiceNumber()))
                .customerName(normalizeString(invoice.getCustomerName()))
                .amount(BigDecimal.valueOf(invoice.getAmount()))
                .currency(normalizeCurrency(invoice.getCurrency()))
                .status(normalizeStatus(invoice.getStatus()))
                .dueDate(parseDueDate(invoice.getDueDate()))
                .rawData(rawJson)
                .build();
    }

    private String normalizeString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim().toUpperCase();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toLowerCase();
    }

    private LocalDate parseDueDate(String dueDate) {
        if (dueDate == null || dueDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dueDate.trim());
        } catch (Exception ex) {
            log.warn("Failed to parse due date '{}': {}", dueDate, ex.getMessage());
            return null;
        }
    }
}
