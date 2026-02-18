package com.dataplatform.service;

import com.dataplatform.dto.TransformedProduct;
import com.dataplatform.model.ValidatedProduct;
import com.dataplatform.repository.ValidatedProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLoadService {

    private final ValidatedProductRepository validatedProductRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void loadProduct(TransformedProduct product) {
        Optional<ValidatedProduct> existing = validatedProductRepository
                .findByExternalId(product.getExternalId());

        if (existing.isPresent()) {
            ValidatedProduct vp = existing.get();
            vp.setSku(product.getSku());
            vp.setName(product.getName());
            vp.setDescription(product.getDescription());
            vp.setCategory(product.getCategory());
            vp.setUnitPrice(product.getUnitPrice());
            vp.setQuantity(product.getQuantity());
            vp.setWarehouse(product.getWarehouse());
            vp.setValidatedAt(LocalDateTime.now());
            validatedProductRepository.save(vp);
            log.debug("Updated validated product: {}", product.getExternalId());
        } else {
            ValidatedProduct vp = ValidatedProduct.builder()
                    .externalId(product.getExternalId())
                    .sku(product.getSku())
                    .name(product.getName())
                    .description(product.getDescription())
                    .category(product.getCategory())
                    .unitPrice(product.getUnitPrice())
                    .quantity(product.getQuantity())
                    .warehouse(product.getWarehouse())
                    .build();
            validatedProductRepository.save(vp);
            log.debug("Inserted validated product: {}", product.getExternalId());
        }

        jdbcTemplate.update("EXEC [final].upsert_products ?, ?, ?, ?, ?, ?, ?, ?, ?",
                product.getExternalId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getUnitPrice(),
                product.getQuantity(),
                product.getWarehouse(),
                "ERP");
        log.debug("Upserted final product: {}", product.getExternalId());
    }
}
