package com.dataplatform.repository;

import com.dataplatform.model.FinalInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalInvoiceRepository extends JpaRepository<FinalInvoice, Long> {
    Optional<FinalInvoice> findByExternalId(String externalId);
}
