package com.iothealth.backend.dto.vitalsign;

import java.math.BigDecimal;
import java.time.Instant;

public record VitalSignResponse(
        Long id,
        Long patientId,
        String patientFullName,
        Long deviceId,
        String deviceCode,
        Integer heartRate,
        BigDecimal temperature,
        Integer spo2,
        Instant recordedAt,
        Instant createdAt
) {
}