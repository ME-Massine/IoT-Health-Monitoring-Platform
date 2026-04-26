package com.iothealth.backend.dto.device;

import com.iothealth.backend.entity.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceRequest(

        @NotBlank(message = "Device code is required")
        @Size(max = 50, message = "Device code must not exceed 50 characters")
        String deviceCode,

        @NotBlank(message = "Device type is required")
        @Size(max = 100, message = "Device type must not exceed 100 characters")
        String type,

        DeviceStatus status,

        @NotNull(message = "Patient ID is required")
        Long patientId
) {
}