package com.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingInvoiceResponse {

    private String id;
    private String invoiceNumber;
    private String customerName;
    private double amount;
    private String currency;
    private String status;
    private String dueDate;
    private List<Map<String, Object>> lineItems;
}
