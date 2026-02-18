package com.dataplatform.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {

    private boolean valid;
    private TransformedCustomer customer;
    private List<String> errors;
}
