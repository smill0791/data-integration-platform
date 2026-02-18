package com.dataplatform.transformer;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.TransformedCustomer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTransformationServiceTest {

    private CustomerTransformationService transformationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        transformationService = new CustomerTransformationService(objectMapper);
    }

    @Test
    void transform_happyPath_shouldNormalizeAllFields() throws Exception {
        String json = objectMapper.writeValueAsString(buildCustomer("CRM-001", "  Alice Smith  ",
                "  Alice@Example.COM  ", "(555) 123-4567", "123 Main St", "Springfield", "IL", "62701"));

        TransformedCustomer result = transformationService.transform(json);

        assertThat(result.getExternalId()).isEqualTo("CRM-001");
        assertThat(result.getName()).isEqualTo("Alice Smith");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getPhone()).isEqualTo("5551234567");
        assertThat(result.getAddress()).isEqualTo("123 Main St, Springfield, IL 62701");
        assertThat(result.getRawData()).isNotNull();
    }

    @Test
    void transform_nullPhone_shouldReturnNullPhone() throws Exception {
        String json = objectMapper.writeValueAsString(buildCustomer("CRM-002", "Bob", "bob@test.com",
                null, "456 Oak Ave", "Chicago", "IL", "60601"));

        TransformedCustomer result = transformationService.transform(json);

        assertThat(result.getPhone()).isNull();
        assertThat(result.getName()).isEqualTo("Bob");
    }

    @Test
    void transform_nullEmail_shouldReturnNullEmail() throws Exception {
        String json = objectMapper.writeValueAsString(buildCustomer("CRM-003", "Carol", null,
                "555-0100", "789 Pine Rd", "Denver", "CO", "80201"));

        TransformedCustomer result = transformationService.transform(json);

        assertThat(result.getEmail()).isNull();
        assertThat(result.getPhone()).isEqualTo("5550100");
    }

    @Test
    void transform_nullAddress_shouldReturnNullAddress() throws Exception {
        CrmCustomerResponse crm = CrmCustomerResponse.builder()
                .id("CRM-004")
                .name("Dave")
                .email("dave@test.com")
                .phone("555-0200")
                .address(null)
                .build();
        String json = objectMapper.writeValueAsString(crm);

        TransformedCustomer result = transformationService.transform(json);

        assertThat(result.getAddress()).isNull();
    }

    @Test
    void transform_partialAddress_shouldFlattenAvailableFields() throws Exception {
        String json = objectMapper.writeValueAsString(buildCustomer("CRM-005", "Eve", "eve@test.com",
                "555-0300", null, "Austin", "TX", null));

        TransformedCustomer result = transformationService.transform(json);

        assertThat(result.getAddress()).isEqualTo("Austin, TX");
    }

    @Test
    void transform_invalidJson_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> transformationService.transform("not valid json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to parse raw customer JSON");
    }

    @Test
    void transform_mixedCaseEmail_shouldLowercase() throws Exception {
        String json = objectMapper.writeValueAsString(buildCustomer("CRM-006", "Frank",
                "Frank.Jones@BigCorp.COM", "555-0400", "100 Elm St", "Boston", "MA", "02101"));

        TransformedCustomer result = transformationService.transform(json);

        assertThat(result.getEmail()).isEqualTo("frank.jones@bigcorp.com");
    }

    @Test
    void transform_variousPhoneFormats_shouldStripNonDigits() throws Exception {
        String json = objectMapper.writeValueAsString(buildCustomer("CRM-007", "Grace",
                "grace@test.com", "+1 (555) 987-6543", "200 Maple Dr", "Seattle", "WA", "98101"));

        TransformedCustomer result = transformationService.transform(json);

        assertThat(result.getPhone()).isEqualTo("15559876543");
    }

    private CrmCustomerResponse buildCustomer(String id, String name, String email, String phone,
                                               String street, String city, String state, String zipCode) {
        return CrmCustomerResponse.builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .address(CrmCustomerResponse.Address.builder()
                        .street(street)
                        .city(city)
                        .state(state)
                        .zipCode(zipCode)
                        .build())
                .lastUpdated("2024-01-15T10:30:00Z")
                .build();
    }
}
