package com.dataplatform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesforceQueryResult {

    private int totalSize;
    private boolean done;
    private String nextRecordsUrl;
    private List<SalesforceContact> records;
}
