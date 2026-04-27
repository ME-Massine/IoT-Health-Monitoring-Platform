package com.iothealth.backend.service;

import com.iothealth.backend.entity.Alert;
import com.iothealth.backend.entity.AlertSeverity;
import com.iothealth.backend.entity.AlertType;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.Patient;
import com.iothealth.backend.entity.VitalSign;
import com.iothealth.backend.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.iothealth.backend.dto.alert.AlertResponse;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.mapper.AlertMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AlertService {

    private static final int LOW_HEART_RATE_CRITICAL = 50;
    private static final int HIGH_HEART_RATE_WARNING = 110;
    private static final int HIGH_HEART_RATE_CRITICAL = 120;

    private static final BigDecimal LOW_TEMPERATURE_CRITICAL = BigDecimal.valueOf(35.0);
    private static final BigDecimal HIGH_TEMPERATURE_WARNING = BigDecimal.valueOf(37.8);
    private static final BigDecimal HIGH_TEMPERATURE_CRITICAL = BigDecimal.valueOf(38.0);

    private static final int LOW_SPO2_WARNING = 94;
    private static final int LOW_SPO2_CRITICAL = 92;

    private final AlertRepository alertRepository;

    public List<Alert> detectAndCreateAlerts(VitalSign vitalSign) {
        List<Alert> alerts = detectAlerts(vitalSign);

        if (alerts.isEmpty()) {
            return List.of();
        }

        return alertRepository.saveAll(alerts);
    }

    private List<Alert> detectAlerts(VitalSign vitalSign) {
        List<Alert> alerts = new ArrayList<>();

        detectHeartRateAlert(vitalSign, alerts);
        detectTemperatureAlert(vitalSign, alerts);
        detectSpo2Alert(vitalSign, alerts);

        return alerts;
    }

    private void detectHeartRateAlert(VitalSign vitalSign, List<Alert> alerts) {
        Integer heartRate = vitalSign.getHeartRate();

        if (heartRate == null) {
            return;
        }

        if (heartRate < LOW_HEART_RATE_CRITICAL) {
            alerts.add(buildAlert(
                    vitalSign,
                    AlertType.LOW_HEART_RATE,
                    AlertSeverity.CRITICAL,
                    "Critical low heart rate detected: " + heartRate + " bpm"
            ));
        } else if (heartRate > HIGH_HEART_RATE_CRITICAL) {
            alerts.add(buildAlert(
                    vitalSign,
                    AlertType.HIGH_HEART_RATE,
                    AlertSeverity.CRITICAL,
                    "Critical high heart rate detected: " + heartRate + " bpm"
            ));
        } else if (heartRate >= HIGH_HEART_RATE_WARNING) {
            alerts.add(buildAlert(
                    vitalSign,
                    AlertType.HIGH_HEART_RATE,
                    AlertSeverity.WARNING,
                    "Warning high heart rate detected: " + heartRate + " bpm"
            ));
        }
    }

    private void detectTemperatureAlert(VitalSign vitalSign, List<Alert> alerts) {
        BigDecimal temperature = vitalSign.getTemperature();

        if (temperature == null) {
            return;
        }

        if (temperature.compareTo(LOW_TEMPERATURE_CRITICAL) < 0) {
            alerts.add(buildAlert(
                    vitalSign,
                    AlertType.LOW_TEMPERATURE,
                    AlertSeverity.CRITICAL,
                    "Critical low temperature detected: " + temperature + " °C"
            ));
        } else if (temperature.compareTo(HIGH_TEMPERATURE_CRITICAL) > 0) {
            alerts.add(buildAlert(
                    vitalSign,
                    AlertType.HIGH_TEMPERATURE,
                    AlertSeverity.CRITICAL,
                    "Critical high temperature detected: " + temperature + " °C"
            ));
        } else if (temperature.compareTo(HIGH_TEMPERATURE_WARNING) >= 0) {
            alerts.add(buildAlert(
                    vitalSign,
                    AlertType.HIGH_TEMPERATURE,
                    AlertSeverity.WARNING,
                    "Warning high temperature detected: " + temperature + " °C"
            ));
        }
    }

    private void detectSpo2Alert(VitalSign vitalSign, List<Alert> alerts) {
        Integer spo2 = vitalSign.getSpo2();

        if (spo2 == null) {
            return;
        }

        if (spo2 <= LOW_SPO2_CRITICAL) {
            alerts.add(buildAlert(
                    vitalSign,
                    AlertType.LOW_SPO2,
                    AlertSeverity.CRITICAL,
                    "Critical low SpO2 detected: " + spo2 + "%"
            ));
        } else if (spo2 <= LOW_SPO2_WARNING) {
            alerts.add(buildAlert(
                    vitalSign,
                    AlertType.LOW_SPO2,
                    AlertSeverity.WARNING,
                    "Warning low SpO2 detected: " + spo2 + "%"
            ));
        }
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlerts() {
        return alertRepository.findAll()
                .stream()
                .map(AlertMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getUnresolvedAlerts() {
        return alertRepository.findByResolvedFalseOrderByCreatedAtDesc()
                .stream()
                .map(AlertMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByPatientId(Long patientId) {
        return alertRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(AlertMapper::toResponse)
                .toList();
    }

    public AlertResponse resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));

        alert.resolve();

        Alert savedAlert = alertRepository.save(alert);

        return AlertMapper.toResponse(savedAlert);
    }

    private Alert buildAlert(
            VitalSign vitalSign,
            AlertType type,
            AlertSeverity severity,
            String message
    ) {
        Patient patient = vitalSign.getPatient();
        Device device = vitalSign.getDevice();

        return Alert.builder()
                .type(type)
                .severity(severity)
                .message(message)
                .resolved(false)
                .patient(patient)
                .device(device)
                .vitalSign(vitalSign)
                .build();
    }
}