package com.iothealth.backend.controller;

import com.iothealth.backend.dto.device.DeviceRequest;
import com.iothealth.backend.dto.device.DeviceResponse;
import com.iothealth.backend.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Register, retrieve, update, and delete IoT devices")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    @Operation(summary = "Create a device")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse createDevice(@Valid @RequestBody DeviceRequest request) {
        return deviceService.createDevice(request);
    }

    @GetMapping
    @Operation(summary = "Get all devices")
    public List<DeviceResponse> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a device by ID")
    public DeviceResponse getDeviceById(@PathVariable Long id) {
        return deviceService.getDeviceById(id);
    }

    @GetMapping("/code/{deviceCode}")
    @Operation(summary = "Get a device by code")
    public DeviceResponse getDeviceByCode(@PathVariable String deviceCode) {
        return deviceService.getDeviceByCode(deviceCode);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get a device by patient ID")
    public DeviceResponse getDeviceByPatientId(@PathVariable Long patientId) {
        return deviceService.getDeviceByPatientId(patientId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a device")
    public DeviceResponse updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody DeviceRequest request
    ) {
        return deviceService.updateDevice(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a device")
    public void deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
    }
}