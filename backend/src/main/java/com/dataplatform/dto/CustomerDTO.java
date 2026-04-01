package com.dataplatform.dto;

import com.dataplatform.model.FinalCustomer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerDTO {

    private Long id;
    private String externalId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String sourceSystem;
    private LocalDateTime firstSyncedAt;
    private LocalDateTime lastSyncedAt;

    public static CustomerDTO fromEntity(FinalCustomer c) {
        return CustomerDTO.builder()
                .id(c.getId())
                .externalId(c.getExternalId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .sourceSystem(c.getSourceSystem())
                .firstSyncedAt(c.getFirstSyncedAt())
                .lastSyncedAt(c.getLastSyncedAt())
                .build();
    }
}
