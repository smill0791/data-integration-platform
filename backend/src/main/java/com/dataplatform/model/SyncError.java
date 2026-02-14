package com.dataplatform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_errors", schema = "audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_job_id", nullable = false)
    private SyncJob syncJob;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "VARCHAR(MAX)")
    private String errorMessage;

    @Column(name = "failed_record", columnDefinition = "VARCHAR(MAX)")
    private String failedRecord;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
