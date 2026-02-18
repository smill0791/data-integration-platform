package com.dataplatform.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformedCustomer {

    private String externalId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String rawData;
}
