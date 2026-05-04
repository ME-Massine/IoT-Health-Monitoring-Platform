package com.iothealth.backend.controller;

import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import com.iothealth.backend.service.VitalSignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vitals")
@RequiredArgsConstructor
@Tag(name = "Vital Signs", description = "Ingest and retrieve patient vital sign readings")
public class VitalSignController {

    private final VitalSignService vitalSignService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest a vital sign reading from a device")
    public VitalSignResponse ingestVitalSign(@Valid @RequestBody VitalSignRequest request) {
        return vitalSignService.ingestVitalSign(request);
    }

    @GetMapping("/patient/{patientId}/latest")
    @Operation(summary = "Get the latest vital sign reading for a patient")
    public VitalSignResponse getLatestVitalSignByPatientId(@PathVariable Long patientId) {
        return vitalSignService.getLatestVitalSignByPatientId(patientId);
    }

    @GetMapping("/patient/{patientId}/history")
    @Operation(summary = "Get vital sign history for a patient",
            description = "Returns up to 500 readings ordered by recordedAt descending. " +
                    "Optionally filter by time range using 'from' and 'to' (both required if used).")
    public List<VitalSignResponse> getVitalSignHistoryByPatientId(
            @PathVariable Long patientId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return vitalSignService.getVitalSignHistoryByPatientId(patientId, from, to, limit);
    }
}