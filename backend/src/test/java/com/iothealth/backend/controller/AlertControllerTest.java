package com.iothealth.backend.controller;

import com.iothealth.backend.dto.alert.AlertResponse;
import com.iothealth.backend.entity.AlertSeverity;
import com.iothealth.backend.entity.AlertType;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    private AlertResponse alertResponse;

    @BeforeEach
    void setUp() {
        alertResponse = new AlertResponse(
                1L, AlertType.HIGH_HEART_RATE, AlertSeverity.WARNING,
                "Warning high heart rate detected: 115 bpm",
                false, Instant.now(), null, null,
                1L, "Sara El Amrani",
                1L, "DEV-001", 1L
        );
    }

    // --- GET /api/v1/alerts ---

    @Test
    void getAllAlerts_returns200WithList() throws Exception {
        when(alertService.getAllAlerts()).thenReturn(List.of(alertResponse));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("HIGH_HEART_RATE"))
                .andExpect(jsonPath("$[0].severity").value("WARNING"))
                .andExpect(jsonPath("$[0].resolved").value(false));
    }

    @Test
    void getAllAlerts_empty_returns200WithEmptyList() throws Exception {
        when(alertService.getAllAlerts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- GET /api/v1/alerts/unresolved ---

    @Test
    void getUnresolvedAlerts_returns200WithList() throws Exception {
        when(alertService.getUnresolvedAlerts()).thenReturn(List.of(alertResponse));

        mockMvc.perform(get("/api/v1/alerts/unresolved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].resolved").value(false));
    }

    // --- GET /api/v1/alerts/patient/{patientId} ---

    @Test
    void getAlertsByPatientId_returns200WithList() throws Exception {
        when(alertService.getAlertsByPatientId(1L)).thenReturn(List.of(alertResponse));

        mockMvc.perform(get("/api/v1/alerts/patient/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].patientId").value(1L));
    }

    @Test
    void getAlertsByPatientId_noAlerts_returns200WithEmptyList() throws Exception {
        when(alertService.getAlertsByPatientId(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alerts/patient/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- PUT /api/v1/alerts/{id}/resolve ---

    @Test
    void resolveAlert_returns200WithResolvedAlert() throws Exception {
        AlertResponse resolved = new AlertResponse(
                1L, AlertType.HIGH_HEART_RATE, AlertSeverity.WARNING,
                "Warning high heart rate detected: 115 bpm",
                true, alertResponse.createdAt(), Instant.now(), null,
                1L, "Sara El Amrani",
                1L, "DEV-001", 1L
        );

        when(alertService.resolveAlert(1L)).thenReturn(resolved);

        mockMvc.perform(put("/api/v1/alerts/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved").value(true))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
    }

    @Test
    void resolveAlert_notFound_returns404() throws Exception {
        when(alertService.resolveAlert(99L))
                .thenThrow(new ResourceNotFoundException("Alert not found with id: 99"));

        mockMvc.perform(put("/api/v1/alerts/99/resolve"))
                .andExpect(status().isNotFound());
    }
}