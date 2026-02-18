package com.dataplatform.repository;

import com.dataplatform.model.FinalProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalProductRepository extends JpaRepository<FinalProduct, Long> {
    Optional<FinalProduct> findByExternalId(String externalId);
}
