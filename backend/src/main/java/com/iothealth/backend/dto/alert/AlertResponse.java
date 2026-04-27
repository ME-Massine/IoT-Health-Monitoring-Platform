package com.iothealth.backend.dto.alert;

import com.iothealth.backend.entity.AlertSeverity;
import com.iothealth.backend.entity.AlertType;

import java.time.Instant;

public record AlertResponse(
        Long id,
        AlertType type,
        AlertSeverity severity,
        String message,
        boolean resolved,
        Instant createdAt,
        Instant resolvedAt,
        Long patientId,
        String patientFullName,
        Long deviceId,
        String deviceCode,
        Long vitalSignId
) {
}