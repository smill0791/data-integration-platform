package com.dataplatform.service;

import com.dataplatform.dto.TransformedCustomer;
import com.dataplatform.model.ValidatedCustomer;
import com.dataplatform.repository.ValidatedCustomerRepository;
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
public class CustomerLoadService {

    private final ValidatedCustomerRepository validatedCustomerRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void loadCustomer(TransformedCustomer customer) {
        Optional<ValidatedCustomer> existing = validatedCustomerRepository
                .findByExternalId(customer.getExternalId());

        if (existing.isPresent()) {
            ValidatedCustomer vc = existing.get();
            vc.setName(customer.getName());
            vc.setEmail(customer.getEmail());
            vc.setPhone(customer.getPhone());
            vc.setAddress(customer.getAddress());
            vc.setValidatedAt(LocalDateTime.now());
            validatedCustomerRepository.save(vc);
            log.debug("Updated validated customer: {}", customer.getExternalId());
        } else {
            ValidatedCustomer vc = ValidatedCustomer.builder()
                    .externalId(customer.getExternalId())
                    .name(customer.getName())
                    .email(customer.getEmail())
                    .phone(customer.getPhone())
                    .address(customer.getAddress())
                    .build();
            validatedCustomerRepository.save(vc);
            log.debug("Inserted validated customer: {}", customer.getExternalId());
        }

        jdbcTemplate.update("EXEC [final].upsert_customers ?, ?, ?, ?, ?, ?",
                customer.getExternalId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                "CRM");
        log.debug("Upserted final customer: {}", customer.getExternalId());
    }
}
