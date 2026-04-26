package com.iothealth.backend.dto.device;

import com.iothealth.backend.entity.DeviceStatus;

import java.time.Instant;

public record DeviceResponse(
        Long id,
        String deviceCode,
        String type,
        DeviceStatus status,
        Long patientId,
        String patientFullName,
        Instant createdAt,
        Instant updatedAt
) {
}