package com.iothealth.backend.mapper;

import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.Patient;
import com.iothealth.backend.entity.VitalSign;

public final class VitalSignMapper {

    private VitalSignMapper() {
    }

    public static VitalSign toEntity(VitalSignRequest request, Device device) {
        Patient patient = device.getPatient();

        return VitalSign.builder()
                .device(device)
                .patient(patient)
                .heartRate(request.heartRate())
                .temperature(request.temperature())
                .spo2(request.spo2())
                .recordedAt(request.recordedAt())
                .build();
    }

    public static VitalSignResponse toResponse(VitalSign vitalSign) {
        Patient patient = vitalSign.getPatient();
        Device device = vitalSign.getDevice();

        return new VitalSignResponse(
                vitalSign.getId(),
                patient != null ? patient.getId() : null,
                formatPatientFullName(patient),
                device != null ? device.getId() : null,
                device != null ? device.getDeviceCode() : null,
                vitalSign.getHeartRate(),
                vitalSign.getTemperature(),
                vitalSign.getSpo2(),
                vitalSign.getRecordedAt(),
                vitalSign.getCreatedAt()
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