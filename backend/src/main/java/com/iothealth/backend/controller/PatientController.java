package com.iothealth.backend.controller;

import com.iothealth.backend.dto.patient.PatientRequest;
import com.iothealth.backend.dto.patient.PatientResponse;
import com.iothealth.backend.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse createPatient(@Valid @RequestBody PatientRequest request) {
        return patientService.createPatient(request);
    }

    @GetMapping
    public List<PatientResponse> getAllPatients() {
        return patientService.getAllPatients();
    }

    @GetMapping("/{id}")
    public PatientResponse getPatientById(@PathVariable Long id) {
        return patientService.getPatientById(id);
    }

    @PutMapping("/{id}")
    public PatientResponse updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request
    ) {
        return patientService.updatePatient(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
    }
}