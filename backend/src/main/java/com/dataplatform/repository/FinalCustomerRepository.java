package com.dataplatform.repository;

import com.dataplatform.model.FinalCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalCustomerRepository extends JpaRepository<FinalCustomer, Long> {
    Optional<FinalCustomer> findByExternalId(String externalId);
}
