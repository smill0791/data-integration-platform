package com.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncMessage {

    private Long jobId;
    private String sourceName;
    private String syncType;

    /**
     * Optional subset of external record IDs to process. Null/empty means a full
     * sync; a populated list scopes the job to those records (used by retry).
     */
    private List<String> recordIds;
}
