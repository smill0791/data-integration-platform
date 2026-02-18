package com.dataplatform.service;

import com.dataplatform.dto.TransformedCustomer;
import com.dataplatform.model.ValidatedCustomer;
import com.dataplatform.repository.ValidatedCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerLoadServiceTest {

    @Mock
    private ValidatedCustomerRepository validatedCustomerRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CustomerLoadService customerLoadService;

    private TransformedCustomer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = TransformedCustomer.builder()
                .externalId("CRM-001")
                .name("Alice Smith")
                .email("alice@example.com")
                .phone("5551234567")
                .address("123 Main St, Springfield, IL 62701")
                .build();
    }

    @Test
    void loadCustomer_newRecord_shouldSaveAndUpsert() {
        when(validatedCustomerRepository.findByExternalId("CRM-001")).thenReturn(Optional.empty());
        when(validatedCustomerRepository.save(any(ValidatedCustomer.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        customerLoadService.loadCustomer(sampleCustomer);

        verify(validatedCustomerRepository).save(argThat(vc ->
                "CRM-001".equals(vc.getExternalId()) && "Alice Smith".equals(vc.getName())));
        verify(jdbcTemplate).update(eq("EXEC [final].upsert_customers ?, ?, ?, ?, ?, ?"),
                eq("CRM-001"), eq("Alice Smith"), eq("alice@example.com"),
                eq("5551234567"), eq("123 Main St, Springfield, IL 62701"), eq("CRM"));
    }

    @Test
    void loadCustomer_existingRecord_shouldUpdateAndUpsert() {
        ValidatedCustomer existing = ValidatedCustomer.builder()
                .id(1L)
                .externalId("CRM-001")
                .name("Old Name")
                .email("old@example.com")
                .build();
        when(validatedCustomerRepository.findByExternalId("CRM-001")).thenReturn(Optional.of(existing));
        when(validatedCustomerRepository.save(any(ValidatedCustomer.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        customerLoadService.loadCustomer(sampleCustomer);

        verify(validatedCustomerRepository).save(argThat(vc ->
                vc.getId() != null && "Alice Smith".equals(vc.getName())));
        verify(jdbcTemplate).update(anyString(), eq("CRM-001"), eq("Alice Smith"),
                eq("alice@example.com"), eq("5551234567"),
                eq("123 Main St, Springfield, IL 62701"), eq("CRM"));
    }

    @Test
    void loadCustomer_jdbcFailure_shouldPropagate() {
        when(validatedCustomerRepository.findByExternalId("CRM-001")).thenReturn(Optional.empty());
        when(validatedCustomerRepository.save(any(ValidatedCustomer.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> customerLoadService.loadCustomer(sampleCustomer))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB connection lost");
    }
}
