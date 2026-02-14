package com.dataplatform.repository;

import com.dataplatform.model.RawCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawCustomerRepository extends JpaRepository<RawCustomer, Long> {

    List<RawCustomer> findBySyncJobId(Long syncJobId);
}
