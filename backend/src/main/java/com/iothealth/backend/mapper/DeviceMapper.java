package com.iothealth.backend.mapper;

import com.iothealth.backend.dto.device.DeviceRequest;
import com.iothealth.backend.dto.device.DeviceResponse;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.DeviceStatus;
import com.iothealth.backend.entity.Patient;

public final class DeviceMapper {

    private DeviceMapper() {
    }

    public static Device toEntity(DeviceRequest request, Patient patient) {
        return Device.builder()
                .deviceCode(request.deviceCode())
                .type(request.type())
                .status(resolveStatus(request.status()))
                .patient(patient)
                .build();
    }

    public static DeviceResponse toResponse(Device device) {
        Patient patient = device.getPatient();

        return new DeviceResponse(
                device.getId(),
                device.getDeviceCode(),
                device.getType(),
                device.getStatus(),
                patient.getId(),
                patient.getFirstName() + " " + patient.getLastName(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }

    public static void updateEntity(Device device, DeviceRequest request, Patient patient) {
        device.setDeviceCode(request.deviceCode());
        device.setType(request.type());
        device.setStatus(resolveStatus(request.status()));
        device.setPatient(patient);
    }

    private static DeviceStatus resolveStatus(DeviceStatus status) {
        return status != null ? status : DeviceStatus.ACTIVE;
    }
}