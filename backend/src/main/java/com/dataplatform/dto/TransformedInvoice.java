package com.dataplatform.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformedInvoice {

    private String externalId;
    private String invoiceNumber;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDate dueDate;
    private String rawData;
}
