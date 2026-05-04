package com.iothealth.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iothealth.backend.dto.device.DeviceRequest;
import com.iothealth.backend.dto.device.DeviceResponse;
import com.iothealth.backend.entity.DeviceStatus;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviceService deviceService;

    private DeviceResponse deviceResponse;
    private DeviceRequest validRequest;

    @BeforeEach
    void setUp() {
        deviceResponse = new DeviceResponse(
                1L, "DEV-001", "Multi-parameter monitor",
                DeviceStatus.ACTIVE, 1L, "Sara El Amrani",
                Instant.now(), Instant.now()
        );

        validRequest = new DeviceRequest("DEV-001", "Multi-parameter monitor", DeviceStatus.ACTIVE, 1L);
    }

    // --- POST /api/v1/devices ---

    @Test
    void createDevice_validRequest_returns201() throws Exception {
        when(deviceService.createDevice(any(DeviceRequest.class))).thenReturn(deviceResponse);

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceCode").value("DEV-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createDevice_blankDeviceCode_returns400() throws Exception {
        DeviceRequest invalid = new DeviceRequest("", "Monitor", DeviceStatus.ACTIVE, 1L);

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDevice_nullPatientId_returns400() throws Exception {
        DeviceRequest invalid = new DeviceRequest("DEV-001", "Monitor", DeviceStatus.ACTIVE, null);

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/devices ---

    @Test
    void getAllDevices_returns200WithList() throws Exception {
        when(deviceService.getAllDevices()).thenReturn(List.of(deviceResponse));

        mockMvc.perform(get("/api/v1/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].deviceCode").value("DEV-001"));
    }

    // --- GET /api/v1/devices/{id} ---

    @Test
    void getDeviceById_found_returns200() throws Exception {
        when(deviceService.getDeviceById(1L)).thenReturn(deviceResponse);

        mockMvc.perform(get("/api/v1/devices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getDeviceById_notFound_returns404() throws Exception {
        when(deviceService.getDeviceById(99L))
                .thenThrow(new ResourceNotFoundException("Device not found with id: 99"));

        mockMvc.perform(get("/api/v1/devices/99"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/v1/devices/code/{deviceCode} ---

    @Test
    void getDeviceByCode_found_returns200() throws Exception {
        when(deviceService.getDeviceByCode("DEV-001")).thenReturn(deviceResponse);

        mockMvc.perform(get("/api/v1/devices/code/DEV-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceCode").value("DEV-001"));
    }

    @Test
    void getDeviceByCode_notFound_returns404() throws Exception {
        when(deviceService.getDeviceByCode("DEV-999"))
                .thenThrow(new ResourceNotFoundException("Device not found with code: DEV-999"));

        mockMvc.perform(get("/api/v1/devices/code/DEV-999"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/v1/devices/patient/{patientId} ---

    @Test
    void getDeviceByPatientId_found_returns200() throws Exception {
        when(deviceService.getDeviceByPatientId(1L)).thenReturn(deviceResponse);

        mockMvc.perform(get("/api/v1/devices/patient/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(1L));
    }

    // --- PUT /api/v1/devices/{id} ---

    @Test
    void updateDevice_validRequest_returns200() throws Exception {
        when(deviceService.updateDevice(eq(1L), any(DeviceRequest.class)))
                .thenReturn(deviceResponse);

        mockMvc.perform(put("/api/v1/devices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceCode").value("DEV-001"));
    }

    @Test
    void updateDevice_blankType_returns400() throws Exception {
        DeviceRequest invalid = new DeviceRequest("DEV-001", "", DeviceStatus.ACTIVE, 1L);

        mockMvc.perform(put("/api/v1/devices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/v1/devices/{id} ---

    @Test
    void deleteDevice_returns204() throws Exception {
        doNothing().when(deviceService).deleteDevice(1L);

        mockMvc.perform(delete("/api/v1/devices/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDevice_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Device not found with id: 99"))
                .when(deviceService).deleteDevice(99L);

        mockMvc.perform(delete("/api/v1/devices/99"))
                .andExpect(status().isNotFound());
    }
}