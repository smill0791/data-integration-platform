package com.dataplatform.validator;

import com.dataplatform.dto.TransformedProduct;
import com.dataplatform.dto.ValidationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductValidationService {

    public ValidationResult validate(TransformedProduct product) {
        List<String> errors = new ArrayList<>();

        if (product.getExternalId() == null || product.getExternalId().isBlank()) {
            errors.add("external_id is required");
        }

        if (product.getSku() == null || product.getSku().isBlank()) {
            errors.add("sku is required");
        }

        if (product.getName() == null || product.getName().isBlank()) {
            errors.add("name is required");
        }

        if (product.getUnitPrice() != null && product.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("unit_price must be >= 0");
        }

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }
}
