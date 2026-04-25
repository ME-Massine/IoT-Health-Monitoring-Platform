package com.iothealth.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping("/api/v1/health-check")
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "UP",
                "service", "iot-health-backend",
                "timestamp", Instant.now()
        );
    }
}