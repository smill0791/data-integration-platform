package com.dataplatform.dto;

import com.dataplatform.model.SyncJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJobDTO {

    private Long id;
    private String sourceName;
    private String syncType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer recordsProcessed;
    private Integer recordsFailed;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static SyncJobDTO fromEntity(SyncJob job) {
        return SyncJobDTO.builder()
                .id(job.getId())
                .sourceName(job.getSourceName())
                .syncType(job.getSyncType())
                .status(job.getStatus())
                .startTime(job.getStartTime())
                .endTime(job.getEndTime())
                .recordsProcessed(job.getRecordsProcessed())
                .recordsFailed(job.getRecordsFailed())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
