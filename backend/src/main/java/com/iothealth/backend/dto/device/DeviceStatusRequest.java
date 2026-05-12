package com.iothealth.backend.dto.device;

import com.iothealth.backend.entity.DeviceStatus;
import jakarta.validation.constraints.NotNull;

public record DeviceStatusRequest(
        @NotNull(message = "Status is required")
        DeviceStatus status
) {
}
