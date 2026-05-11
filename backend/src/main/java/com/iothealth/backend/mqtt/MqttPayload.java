package com.iothealth.backend.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MqttPayload(
        String deviceCode,
        Integer heartRate,
        BigDecimal temperature,
        Integer spo2,
        Instant recordedAt
) {
}
