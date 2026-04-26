package com.iothealth.backend.controller;

import com.iothealth.backend.dto.device.DeviceRequest;
import com.iothealth.backend.dto.device.DeviceResponse;
import com.iothealth.backend.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse createDevice(@Valid @RequestBody DeviceRequest request) {
        return deviceService.createDevice(request);
    }

    @GetMapping
    public List<DeviceResponse> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{id}")
    public DeviceResponse getDeviceById(@PathVariable Long id) {
        return deviceService.getDeviceById(id);
    }

    @GetMapping("/code/{deviceCode}")
    public DeviceResponse getDeviceByCode(@PathVariable String deviceCode) {
        return deviceService.getDeviceByCode(deviceCode);
    }

    @GetMapping("/patient/{patientId}")
    public DeviceResponse getDeviceByPatientId(@PathVariable Long patientId) {
        return deviceService.getDeviceByPatientId(patientId);
    }

    @PutMapping("/{id}")
    public DeviceResponse updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequest request
    ) {
        return deviceService.updateDevice(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
    }
}