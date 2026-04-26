package com.iothealth.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "vital_signs",
        indexes = {
                @Index(name = "idx_vital_signs_patient_recorded_at", columnList = "patient_id, recorded_at"),
                @Index(name = "idx_vital_signs_device_recorded_at", columnList = "device_id, recorded_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "heart_rate", nullable = false)
    private Integer heartRate;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(nullable = false)
    private Integer spo2;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = Instant.now();
        }

        this.createdAt = Instant.now();
    }
}