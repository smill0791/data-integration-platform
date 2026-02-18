package com.dataplatform.repository;

import com.dataplatform.model.RawProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawProductRepository extends JpaRepository<RawProduct, Long> {

    List<RawProduct> findBySyncJobId(Long syncJobId);
}
