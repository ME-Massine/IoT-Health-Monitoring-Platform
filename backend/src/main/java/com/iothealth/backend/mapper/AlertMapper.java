package com.iothealth.backend.mapper;

import com.iothealth.backend.dto.alert.AlertResponse;
import com.iothealth.backend.entity.Alert;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.Patient;
import com.iothealth.backend.entity.VitalSign;

public final class AlertMapper {

    private AlertMapper() {
    }

    public static AlertResponse toResponse(Alert alert) {
        Patient patient = alert.getPatient();
        Device device = alert.getDevice();
        VitalSign vitalSign = alert.getVitalSign();

        return new AlertResponse(
                alert.getId(),
                alert.getType(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.isResolved(),
                alert.getCreatedAt(),
                alert.getResolvedAt(),
                patient != null ? patient.getId() : null,
                formatPatientFullName(patient),
                device != null ? device.getId() : null,
                device != null ? device.getDeviceCode() : null,
                vitalSign != null ? vitalSign.getId() : null
        );
    }

    private static String formatPatientFullName(Patient patient) {
        if (patient == null) {
            return null;
        }

        String firstName = patient.getFirstName() != null ? patient.getFirstName().trim() : "";
        String lastName = patient.getLastName() != null ? patient.getLastName().trim() : "";

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? null : fullName;
    }
}