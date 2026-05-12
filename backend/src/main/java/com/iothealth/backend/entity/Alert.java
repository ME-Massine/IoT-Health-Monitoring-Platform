package com.iothealth.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "alerts",
        indexes = {
                @Index(name = "idx_alerts_patient_created_at", columnList = "patient_id, created_at"),
                @Index(name = "idx_alerts_resolved_created_at", columnList = "resolved, created_at"),
                @Index(name = "idx_alerts_severity_created_at", columnList = "severity, created_at"),
                @Index(name = "idx_alerts_type_created_at", columnList = "type, created_at"),
                @Index(name = "idx_alerts_vital_sign_created_at", columnList = "vital_sign_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertSeverity severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vital_sign_id", nullable = false)
    private VitalSign vitalSign;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void resolve() {
        if (!this.resolved) {
            this.resolved = true;
            this.resolvedAt = Instant.now();
        }
    }

    public void acknowledge() {
        if (this.acknowledgedAt == null) {
            this.acknowledgedAt = Instant.now();
        }
    }
}