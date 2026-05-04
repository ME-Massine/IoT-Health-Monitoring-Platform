package com.iothealth.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iothealth.backend.dto.patient.PatientRequest;
import com.iothealth.backend.dto.patient.PatientResponse;
import com.iothealth.backend.entity.Gender;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.service.PatientService;
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

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    private PatientResponse patientResponse;
    private PatientRequest validRequest;

    @BeforeEach
    void setUp() {
        patientResponse = new PatientResponse(
                1L, "Sara", "El Amrani", 42, Gender.FEMALE,
                "A-102", "Cardiac observation",
                Instant.now(), Instant.now()
        );

        validRequest = new PatientRequest(
                "Sara", "El Amrani", 42, Gender.FEMALE, "A-102", "Cardiac observation"
        );
    }

    // --- POST /api/v1/patients ---

    @Test
    void createPatient_validRequest_returns201() throws Exception {
        when(patientService.createPatient(any(PatientRequest.class))).thenReturn(patientResponse);

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("Sara"))
                .andExpect(jsonPath("$.roomNumber").value("A-102"));
    }

    @Test
    void createPatient_blankFirstName_returns400() throws Exception {
        PatientRequest invalid = new PatientRequest(
                "", "El Amrani", 42, Gender.FEMALE, "A-102", null
        );

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPatient_nullGender_returns400() throws Exception {
        PatientRequest invalid = new PatientRequest(
                "Sara", "El Amrani", 42, null, "A-102", null
        );

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPatient_ageTooHigh_returns400() throws Exception {
        PatientRequest invalid = new PatientRequest(
                "Sara", "El Amrani", 150, Gender.FEMALE, "A-102", null
        );

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/patients ---

    @Test
    void getAllPatients_returns200WithList() throws Exception {
        when(patientService.getAllPatients()).thenReturn(List.of(patientResponse));

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lastName").value("El Amrani"));
    }

    // --- GET /api/v1/patients/{id} ---

    @Test
    void getPatientById_found_returns200() throws Exception {
        when(patientService.getPatientById(1L)).thenReturn(patientResponse);

        mockMvc.perform(get("/api/v1/patients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getPatientById_notFound_returns404() throws Exception {
        when(patientService.getPatientById(99L))
                .thenThrow(new ResourceNotFoundException("Patient not found with id: 99"));

        mockMvc.perform(get("/api/v1/patients/99"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/v1/patients/{id} ---

    @Test
    void updatePatient_validRequest_returns200() throws Exception {
        when(patientService.updatePatient(eq(1L), any(PatientRequest.class)))
                .thenReturn(patientResponse);

        mockMvc.perform(put("/api/v1/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Sara"));
    }

    @Test
    void updatePatient_blankLastName_returns400() throws Exception {
        PatientRequest invalid = new PatientRequest(
                "Sara", "", 42, Gender.FEMALE, "A-102", null
        );

        mockMvc.perform(put("/api/v1/patients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/v1/patients/{id} ---

    @Test
    void deletePatient_returns204() throws Exception {
        doNothing().when(patientService).deletePatient(1L);

        mockMvc.perform(delete("/api/v1/patients/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePatient_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Patient not found with id: 99"))
                .when(patientService).deletePatient(99L);

        mockMvc.perform(delete("/api/v1/patients/99"))
                .andExpect(status().isNotFound());
    }
}