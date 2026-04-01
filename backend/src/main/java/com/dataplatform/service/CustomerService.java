package com.dataplatform.service;

import com.dataplatform.dto.CustomerDTO;
import com.dataplatform.repository.FinalCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final FinalCustomerRepository finalCustomerRepository;

    public List<CustomerDTO> getCustomers(String source) {
        Sort sort = Sort.by(Sort.Direction.DESC, "lastSyncedAt");
        if (source != null && !source.isBlank()) {
            log.debug("Fetching customers for source={}", source);
            return finalCustomerRepository.findBySourceSystem(source.toUpperCase(), sort)
                    .stream().map(CustomerDTO::fromEntity).toList();
        }
        log.debug("Fetching all customers");
        return finalCustomerRepository.findAll(sort)
                .stream().map(CustomerDTO::fromEntity).toList();
    }
}
