package com.dataplatform.repository;

import com.dataplatform.model.ValidatedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValidatedProductRepository extends JpaRepository<ValidatedProduct, Long> {
    Optional<ValidatedProduct> findByExternalId(String externalId);
}
