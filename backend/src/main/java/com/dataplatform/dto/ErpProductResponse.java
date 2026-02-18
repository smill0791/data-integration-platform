package com.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpProductResponse {

    private String id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private double unitPrice;
    private int quantity;
    private String warehouse;
}
