package com.dataplatform.transformer;

import com.dataplatform.dto.CrmCustomerResponse;
import com.dataplatform.dto.TransformedCustomer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTransformationService {

    private final ObjectMapper objectMapper;

    public TransformedCustomer transform(String rawJson) {
        CrmCustomerResponse crm;
        try {
            crm = objectMapper.readValue(rawJson, CrmCustomerResponse.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse raw customer JSON: " + ex.getMessage(), ex);
        }

        return TransformedCustomer.builder()
                .externalId(crm.getId())
                .name(normalizeString(crm.getName()))
                .email(normalizeEmail(crm.getEmail()))
                .phone(normalizePhone(crm.getPhone()))
                .address(flattenAddress(crm.getAddress()))
                .rawData(rawJson)
                .build();
    }

    private String normalizeString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.replaceAll("[^\\d]", "");
    }

    private String flattenAddress(CrmCustomerResponse.Address address) {
        if (address == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (address.getStreet() != null && !address.getStreet().isBlank()) {
            sb.append(address.getStreet().trim());
        }
        if (address.getCity() != null && !address.getCity().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getCity().trim());
        }
        if (address.getState() != null && !address.getState().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getState().trim());
        }
        if (address.getZipCode() != null && !address.getZipCode().isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(address.getZipCode().trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
