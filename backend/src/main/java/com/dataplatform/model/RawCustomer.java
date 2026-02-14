package com.dataplatform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_customers", schema = "staging")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_job_id", nullable = false)
    private SyncJob syncJob;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "raw_data", columnDefinition = "VARCHAR(MAX)")
    private String rawData;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
