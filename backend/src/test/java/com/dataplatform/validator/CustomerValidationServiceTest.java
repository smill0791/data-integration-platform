package com.dataplatform.validator;

import com.dataplatform.dto.TransformedCustomer;
import com.dataplatform.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerValidationServiceTest {

    private CustomerValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new CustomerValidationService();
    }

    @Test
    void validate_validRecord_shouldReturnValid() {
        TransformedCustomer customer = TransformedCustomer.builder()
                .externalId("CRM-001")
                .name("Alice Smith")
                .email("alice@example.com")
                .phone("5551234567")
                .address("123 Main St, Springfield, IL 62701")
                .build();

        ValidationResult result = validationService.validate(customer);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getCustomer()).isEqualTo(customer);
    }

    @Test
    void validate_missingExternalId_shouldReturnInvalid() {
        TransformedCustomer customer = TransformedCustomer.builder()
                .name("Bob")
                .email("bob@test.com")
                .build();

        ValidationResult result = validationService.validate(customer);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("external_id is required");
    }

    @Test
    void validate_missingName_shouldReturnInvalid() {
        TransformedCustomer customer = TransformedCustomer.builder()
                .externalId("CRM-002")
                .email("carol@test.com")
                .build();

        ValidationResult result = validationService.validate(customer);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("name is required");
    }

    @Test
    void validate_invalidEmail_shouldReturnInvalid() {
        TransformedCustomer customer = TransformedCustomer.builder()
                .externalId("CRM-003")
                .name("Dave")
                .email("not-an-email")
                .build();

        ValidationResult result = validationService.validate(customer);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("email format is invalid"));
    }

    @Test
    void validate_nullEmail_shouldBeValid() {
        TransformedCustomer customer = TransformedCustomer.builder()
                .externalId("CRM-004")
                .name("Eve")
                .email(null)
                .build();

        ValidationResult result = validationService.validate(customer);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void validate_multipleErrors_shouldReturnAll() {
        TransformedCustomer customer = TransformedCustomer.builder()
                .email("bad-email")
                .build();

        ValidationResult result = validationService.validate(customer);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(3);
        assertThat(result.getErrors()).contains("external_id is required");
        assertThat(result.getErrors()).contains("name is required");
        assertThat(result.getErrors()).anyMatch(e -> e.contains("email format is invalid"));
    }

    @Test
    void validate_edgeCaseEmailFormats_shouldAcceptValid() {
        TransformedCustomer customer = TransformedCustomer.builder()
                .externalId("CRM-005")
                .name("Frank")
                .email("user.name+tag@sub.domain.com")
                .build();

        ValidationResult result = validationService.validate(customer);

        assertThat(result.isValid()).isTrue();
    }
}
