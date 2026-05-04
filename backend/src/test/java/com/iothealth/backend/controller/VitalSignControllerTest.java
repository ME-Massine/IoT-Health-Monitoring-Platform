package com.iothealth.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.service.VitalSignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VitalSignController.class)
class VitalSignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VitalSignService vitalSignService;

    private VitalSignResponse vitalSignResponse;
    private VitalSignRequest validRequest;

    @BeforeEach
    void setUp() {
        vitalSignResponse = new VitalSignResponse(
                1L, 1L, "Sara El Amrani",
                1L, "DEV-001",
                75, BigDecimal.valueOf(36.8), 98,
                Instant.now(), Instant.now()
        );

        validRequest = new VitalSignRequest(
                "DEV-001", 75, BigDecimal.valueOf(36.8), 98, null
        );
    }

    // --- POST /api/v1/vitals ---

    @Test
    void ingestVitalSign_validRequest_returns201() throws Exception {
        when(vitalSignService.ingestVitalSign(any(VitalSignRequest.class)))
                .thenReturn(vitalSignResponse);

        mockMvc.perform(post("/api/v1/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.heartRate").value(75))
                .andExpect(jsonPath("$.spo2").value(98))
                .andExpect(jsonPath("$.deviceCode").value("DEV-001"));
    }

    @Test
    void ingestVitalSign_blankDeviceCode_returns400() throws Exception {
        VitalSignRequest invalid = new VitalSignRequest(
                "", 75, BigDecimal.valueOf(36.8), 98, null
        );

        mockMvc.perform(post("/api/v1/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestVitalSign_heartRateTooLow_returns400() throws Exception {
        VitalSignRequest invalid = new VitalSignRequest(
                "DEV-001", 20, BigDecimal.valueOf(36.8), 98, null  // min is 30
        );

        mockMvc.perform(post("/api/v1/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestVitalSign_temperatureOutOfRange_returns400() throws Exception {
        VitalSignRequest invalid = new VitalSignRequest(
                "DEV-001", 75, BigDecimal.valueOf(50.0), 98, null  // max is 45.0
        );

        mockMvc.perform(post("/api/v1/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestVitalSign_spo2TooLow_returns400() throws Exception {
        VitalSignRequest invalid = new VitalSignRequest(
                "DEV-001", 75, BigDecimal.valueOf(36.8), 40, null  // min is 50
        );

        mockMvc.perform(post("/api/v1/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/vitals/patient/{patientId}/latest ---

    @Test
    void getLatestVitalSign_found_returns200() throws Exception {
        when(vitalSignService.getLatestVitalSignByPatientId(1L))
                .thenReturn(vitalSignResponse);

        mockMvc.perform(get("/api/v1/vitals/patient/1/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(1L))
                .andExpect(jsonPath("$.heartRate").value(75));
    }

    @Test
    void getLatestVitalSign_notFound_returns404() throws Exception {
        when(vitalSignService.getLatestVitalSignByPatientId(99L))
                .thenThrow(new ResourceNotFoundException("No vital sign readings found for patient id: 99"));

        mockMvc.perform(get("/api/v1/vitals/patient/99/latest"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/v1/vitals/patient/{patientId}/history ---

    @Test
    void getVitalSignHistory_returns200WithList() throws Exception {
        when(vitalSignService.getVitalSignHistoryByPatientId(eq(1L), isNull(), isNull(), eq(100)))
                .thenReturn(List.of(vitalSignResponse));

        mockMvc.perform(get("/api/v1/vitals/patient/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].deviceCode").value("DEV-001"));
    }

    @Test
    void getVitalSignHistory_withLimit_returns200() throws Exception {
        when(vitalSignService.getVitalSignHistoryByPatientId(eq(1L), isNull(), isNull(), eq(10)))
                .thenReturn(List.of(vitalSignResponse));

        mockMvc.perform(get("/api/v1/vitals/patient/1/history")
                        .param("limit", "10"))
                .andExpect(status().isOk());
    }
}