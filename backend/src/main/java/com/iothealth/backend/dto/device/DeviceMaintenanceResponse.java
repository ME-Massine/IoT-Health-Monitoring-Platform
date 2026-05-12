package com.iothealth.backend.dto.device;

import java.time.Instant;

public record DeviceMaintenanceResponse(
        Long id,
        Long deviceId,
        String deviceCode,
        Instant startedAt,
        Instant endedAt,
        String reason
) {
}
