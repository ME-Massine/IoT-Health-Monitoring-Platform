package com.iothealth.backend.controller;

import com.iothealth.backend.dto.alert.AlertResponse;
import com.iothealth.backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<AlertResponse> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/unresolved")
    public List<AlertResponse> getUnresolvedAlerts() {
        return alertService.getUnresolvedAlerts();
    }

    @GetMapping("/patient/{patientId}")
    public List<AlertResponse> getAlertsByPatientId(@PathVariable Long patientId) {
        return alertService.getAlertsByPatientId(patientId);
    }

    @PutMapping("/{id}/resolve")
    public AlertResponse resolveAlert(@PathVariable Long id) {
        return alertService.resolveAlert(id);
    }
}