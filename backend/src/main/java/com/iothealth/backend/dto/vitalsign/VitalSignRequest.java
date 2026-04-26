package com.iothealth.backend.dto.vitalsign;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record VitalSignRequest(

        @NotBlank(message = "Device code is required")
        @Size(max = 50, message = "Device code must not exceed 50 characters")
        String deviceCode,

        @NotNull(message = "Heart rate is required")
        @Min(value = 30, message = "Heart rate must be at least 30 bpm")
        @Max(value = 220, message = "Heart rate must not exceed 220 bpm")
        Integer heartRate,

        @NotNull(message = "Temperature is required")
        @DecimalMin(value = "30.0", message = "Temperature must be at least 30.0°C")
        @DecimalMax(value = "45.0", message = "Temperature must not exceed 45.0°C")
        BigDecimal temperature,

        @NotNull(message = "SpO2 is required")
        @Min(value = 50, message = "SpO2 must be at least 50%")
        @Max(value = 100, message = "SpO2 must not exceed 100%")
        Integer spo2,

        Instant recordedAt
) {
}