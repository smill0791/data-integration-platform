package com.dataplatform.repository;

import com.dataplatform.model.ValidatedInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValidatedInvoiceRepository extends JpaRepository<ValidatedInvoice, Long> {
    Optional<ValidatedInvoice> findByExternalId(String externalId);
}
