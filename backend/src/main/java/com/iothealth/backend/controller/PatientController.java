package com.iothealth.backend.controller;

import com.iothealth.backend.dto.patient.PatientRequest;
import com.iothealth.backend.dto.patient.PatientResponse;
import com.iothealth.backend.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Create, retrieve, update, and delete patients")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a patient")
    public PatientResponse createPatient(@Valid @RequestBody PatientRequest request) {
        return patientService.createPatient(request);
    }

    @GetMapping
    @Operation(summary = "Get all patients")
    public List<PatientResponse> getAllPatients() {
        return patientService.getAllPatients();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a patient by ID")
    public PatientResponse getPatientById(@PathVariable Long id) {
        return patientService.getPatientById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a patient")
    public PatientResponse updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request
    ) {
        return patientService.updatePatient(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a patient")
    public void deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
    }
}