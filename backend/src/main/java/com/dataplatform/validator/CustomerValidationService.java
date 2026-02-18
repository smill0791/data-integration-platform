package com.dataplatform.validator;

import com.dataplatform.dto.TransformedCustomer;
import com.dataplatform.dto.ValidationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CustomerValidationService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public ValidationResult validate(TransformedCustomer customer) {
        List<String> errors = new ArrayList<>();

        if (customer.getExternalId() == null || customer.getExternalId().isBlank()) {
            errors.add("external_id is required");
        }

        if (customer.getName() == null || customer.getName().isBlank()) {
            errors.add("name is required");
        }

        if (customer.getEmail() != null && !customer.getEmail().isBlank()
                && !EMAIL_PATTERN.matcher(customer.getEmail()).matches()) {
            errors.add("email format is invalid: " + customer.getEmail());
        }

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .customer(customer)
                .errors(errors)
                .build();
    }
}
