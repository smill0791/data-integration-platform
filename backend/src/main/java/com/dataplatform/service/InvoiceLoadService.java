package com.dataplatform.service;

import com.dataplatform.dto.TransformedInvoice;
import com.dataplatform.model.ValidatedInvoice;
import com.dataplatform.repository.ValidatedInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceLoadService {

    private final ValidatedInvoiceRepository validatedInvoiceRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void loadInvoice(TransformedInvoice invoice) {
        Optional<ValidatedInvoice> existing = validatedInvoiceRepository
                .findByExternalId(invoice.getExternalId());

        if (existing.isPresent()) {
            ValidatedInvoice vi = existing.get();
            vi.setInvoiceNumber(invoice.getInvoiceNumber());
            vi.setCustomerName(invoice.getCustomerName());
            vi.setAmount(invoice.getAmount());
            vi.setCurrency(invoice.getCurrency());
            vi.setStatus(invoice.getStatus());
            vi.setDueDate(invoice.getDueDate());
            vi.setValidatedAt(LocalDateTime.now());
            validatedInvoiceRepository.save(vi);
            log.debug("Updated validated invoice: {}", invoice.getExternalId());
        } else {
            ValidatedInvoice vi = ValidatedInvoice.builder()
                    .externalId(invoice.getExternalId())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .customerName(invoice.getCustomerName())
                    .amount(invoice.getAmount())
                    .currency(invoice.getCurrency())
                    .status(invoice.getStatus())
                    .dueDate(invoice.getDueDate())
                    .build();
            validatedInvoiceRepository.save(vi);
            log.debug("Inserted validated invoice: {}", invoice.getExternalId());
        }

        jdbcTemplate.update("EXEC [final].upsert_invoices ?, ?, ?, ?, ?, ?, ?, ?",
                invoice.getExternalId(),
                invoice.getInvoiceNumber(),
                invoice.getCustomerName(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getDueDate(),
                "ACCOUNTING");
        log.debug("Upserted final invoice: {}", invoice.getExternalId());
    }
}
