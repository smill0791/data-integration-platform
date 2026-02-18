package com.dataplatform.dto;

import com.dataplatform.model.SyncError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncErrorDTO {

    private Long id;
    private String errorType;
    private String errorMessage;
    private String failedRecord;
    private LocalDateTime occurredAt;

    public static SyncErrorDTO fromEntity(SyncError error) {
        return SyncErrorDTO.builder()
                .id(error.getId())
                .errorType(error.getErrorType())
                .errorMessage(error.getErrorMessage())
                .failedRecord(error.getFailedRecord())
                .occurredAt(error.getOccurredAt())
                .build();
    }
}
