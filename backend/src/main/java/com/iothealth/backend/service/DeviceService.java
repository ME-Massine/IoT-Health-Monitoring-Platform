package com.iothealth.backend.service;

import com.iothealth.backend.dto.device.DeviceMaintenanceResponse;
import com.iothealth.backend.dto.device.DeviceRequest;
import com.iothealth.backend.dto.device.DeviceResponse;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.DeviceMaintenance;
import com.iothealth.backend.entity.DeviceStatus;
import com.iothealth.backend.entity.Patient;
import com.iothealth.backend.exception.BadRequestException;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.mapper.DeviceMapper;
import com.iothealth.backend.repository.DeviceMaintenanceRepository;
import com.iothealth.backend.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMaintenanceRepository maintenanceRepository;
    private final PatientService patientService;

    public DeviceResponse createDevice(DeviceRequest request) {
        validateDeviceCodeIsUnique(request.deviceCode());
        validatePatientHasNoDevice(request.patientId());

        Patient patient = patientService.findPatientEntityById(request.patientId());

        Device device = DeviceMapper.toEntity(request, patient);
        Device savedDevice = deviceRepository.save(device);

        return DeviceMapper.toResponse(savedDevice);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll()
                .stream()
                .map(DeviceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(Long id) {
        Device device = findDeviceEntityById(id);
        return DeviceMapper.toResponse(device);
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceByCode(String deviceCode) {
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with code: " + deviceCode));

        return DeviceMapper.toResponse(device);
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDeviceByPatientId(Long patientId) {
        Device device = deviceRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found for patient id: " + patientId));

        return DeviceMapper.toResponse(device);
    }

    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        Device existingDevice = findDeviceEntityById(id);

        if (!existingDevice.getDeviceCode().equals(request.deviceCode())) {
            validateDeviceCodeIsUnique(request.deviceCode());
        }

        if (!existingDevice.getPatient().getId().equals(request.patientId())) {
            validatePatientHasNoDevice(request.patientId());
        }

        Patient patient = patientService.findPatientEntityById(request.patientId());

        DeviceMapper.updateEntity(existingDevice, request, patient);
        Device updatedDevice = deviceRepository.save(existingDevice);

        return DeviceMapper.toResponse(updatedDevice);
    }

    public void deleteDevice(Long id) {
        Device device = findDeviceEntityById(id);
        deviceRepository.delete(device);
    }

    public DeviceResponse setDeviceStatus(Long id, DeviceStatus newStatus) {
        Device device = findDeviceEntityById(id);
        DeviceStatus oldStatus = device.getStatus();

        if (oldStatus == newStatus) {
            throw new BadRequestException("Device is already in status: " + newStatus);
        }

        if (newStatus == DeviceStatus.MAINTENANCE) {
            DeviceMaintenance window = DeviceMaintenance.builder()
                    .device(device)
                    .startedAt(Instant.now())
                    .build();
            maintenanceRepository.save(window);
        } else if (oldStatus == DeviceStatus.MAINTENANCE) {
            maintenanceRepository.findFirstByDeviceIdAndEndedAtIsNull(id)
                    .ifPresent(w -> {
                        w.setEndedAt(Instant.now());
                        maintenanceRepository.save(w);
                    });
        }

        device.setStatus(newStatus);
        return DeviceMapper.toResponse(deviceRepository.save(device));
    }

    @Transactional(readOnly = true)
    public List<DeviceMaintenanceResponse> getMaintenanceWindows(Long id) {
        findDeviceEntityById(id);
        return maintenanceRepository.findByDeviceIdOrderByStartedAtDesc(id)
                .stream()
                .map(w -> new DeviceMaintenanceResponse(
                        w.getId(),
                        w.getDevice().getId(),
                        w.getDevice().getDeviceCode(),
                        w.getStartedAt(),
                        w.getEndedAt(),
                        w.getReason()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Device findDeviceEntityById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + id));
    }

    private void validateDeviceCodeIsUnique(String deviceCode) {
        if (deviceRepository.existsByDeviceCode(deviceCode)) {
            throw new BadRequestException("Device code already exists: " + deviceCode);
        }
    }

    private void validatePatientHasNoDevice(Long patientId) {
        if (deviceRepository.existsByPatientId(patientId)) {
            throw new BadRequestException("Patient already has an assigned device: patientId=" + patientId);
        }
    }
}