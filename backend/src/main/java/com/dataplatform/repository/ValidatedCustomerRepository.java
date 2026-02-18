package com.dataplatform.repository;

import com.dataplatform.model.ValidatedCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValidatedCustomerRepository extends JpaRepository<ValidatedCustomer, Long> {
    Optional<ValidatedCustomer> findByExternalId(String externalId);
}
