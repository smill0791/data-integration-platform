package com.dataplatform.service;

import com.dataplatform.dto.TransformedInvoice;
import com.dataplatform.model.ValidatedInvoice;
import com.dataplatform.repository.ValidatedInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceLoadServiceTest {

    @Mock private ValidatedInvoiceRepository validatedInvoiceRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private InvoiceLoadService invoiceLoadService;

    private TransformedInvoice sampleInvoice;

    @BeforeEach
    void setUp() {
        sampleInvoice = TransformedInvoice.builder()
                .externalId("ACC-001").invoiceNumber("INV-001").customerName("Acme Corp")
                .amount(BigDecimal.valueOf(1500.50)).currency("USD").status("paid")
                .dueDate(LocalDate.of(2025, 6, 15)).build();
    }

    @Test
    void loadInvoice_newRecord_shouldSaveAndUpsert() {
        when(validatedInvoiceRepository.findByExternalId("ACC-001")).thenReturn(Optional.empty());
        when(validatedInvoiceRepository.save(any(ValidatedInvoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        invoiceLoadService.loadInvoice(sampleInvoice);

        verify(validatedInvoiceRepository).save(argThat(vi ->
                "ACC-001".equals(vi.getExternalId()) && "INV-001".equals(vi.getInvoiceNumber())));
        verify(jdbcTemplate).update(eq("EXEC [final].upsert_invoices ?, ?, ?, ?, ?, ?, ?, ?"),
                eq("ACC-001"), eq("INV-001"), eq("Acme Corp"), eq(BigDecimal.valueOf(1500.50)),
                eq("USD"), eq("paid"), eq(LocalDate.of(2025, 6, 15)), eq("ACCOUNTING"));
    }

    @Test
    void loadInvoice_existingRecord_shouldUpdateAndUpsert() {
        ValidatedInvoice existing = ValidatedInvoice.builder()
                .id(1L).externalId("ACC-001").invoiceNumber("OLD-INV").customerName("Old Name").build();
        when(validatedInvoiceRepository.findByExternalId("ACC-001")).thenReturn(Optional.of(existing));
        when(validatedInvoiceRepository.save(any(ValidatedInvoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        invoiceLoadService.loadInvoice(sampleInvoice);

        verify(validatedInvoiceRepository).save(argThat(vi ->
                vi.getId() != null && "INV-001".equals(vi.getInvoiceNumber()) && "Acme Corp".equals(vi.getCustomerName())));
    }

    @Test
    void loadInvoice_jdbcFailure_shouldPropagate() {
        when(validatedInvoiceRepository.findByExternalId("ACC-001")).thenReturn(Optional.empty());
        when(validatedInvoiceRepository.save(any(ValidatedInvoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> invoiceLoadService.loadInvoice(sampleInvoice))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB connection lost");
    }
}
