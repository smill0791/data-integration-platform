package com.dataplatform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "validated_customers", schema = "validated")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidatedCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @PrePersist
    protected void onCreate() {
        if (validatedAt == null) {
            validatedAt = LocalDateTime.now();
        }
    }
}
