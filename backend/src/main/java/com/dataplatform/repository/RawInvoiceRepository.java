package com.dataplatform.repository;

import com.dataplatform.model.RawInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawInvoiceRepository extends JpaRepository<RawInvoice, Long> {

    List<RawInvoice> findBySyncJobId(Long syncJobId);
}
