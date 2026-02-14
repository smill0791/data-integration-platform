package com.dataplatform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_jobs", schema = "audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "sync_type", nullable = false, length = 50)
    private String syncType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "records_processed")
    @Builder.Default
    private Integer recordsProcessed = 0;

    @Column(name = "records_failed")
    @Builder.Default
    private Integer recordsFailed = 0;

    @Column(name = "error_message", columnDefinition = "VARCHAR(MAX)")
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
    }
}
