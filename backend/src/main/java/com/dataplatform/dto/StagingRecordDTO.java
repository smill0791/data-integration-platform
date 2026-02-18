package com.dataplatform.dto;

import com.dataplatform.model.RawCustomer;
import com.dataplatform.model.RawInvoice;
import com.dataplatform.model.RawProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StagingRecordDTO {

    private Long id;
    private String externalId;
    private String rawData;
    private LocalDateTime receivedAt;

    public static StagingRecordDTO fromRawCustomer(RawCustomer rc) {
        return StagingRecordDTO.builder()
                .id(rc.getId())
                .externalId(rc.getExternalId())
                .rawData(rc.getRawData())
                .receivedAt(rc.getReceivedAt())
                .build();
    }

    public static StagingRecordDTO fromRawProduct(RawProduct rp) {
        return StagingRecordDTO.builder()
                .id(rp.getId())
                .externalId(rp.getExternalId())
                .rawData(rp.getRawData())
                .receivedAt(rp.getReceivedAt())
                .build();
    }

    public static StagingRecordDTO fromRawInvoice(RawInvoice ri) {
        return StagingRecordDTO.builder()
                .id(ri.getId())
                .externalId(ri.getExternalId())
                .rawData(ri.getRawData())
                .receivedAt(ri.getReceivedAt())
                .build();
    }
}
