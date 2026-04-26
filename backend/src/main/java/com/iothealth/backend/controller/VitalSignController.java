package com.iothealth.backend.controller;

import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import com.iothealth.backend.service.VitalSignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vitals")
@RequiredArgsConstructor
public class VitalSignController {

    private final VitalSignService vitalSignService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VitalSignResponse ingestVitalSign(@Valid @RequestBody VitalSignRequest request) {
        return vitalSignService.ingestVitalSign(request);
    }

    @GetMapping("/patient/{patientId}/latest")
    public VitalSignResponse getLatestVitalSignByPatientId(@PathVariable Long patientId) {
        return vitalSignService.getLatestVitalSignByPatientId(patientId);
    }

    @GetMapping("/patient/{patientId}/history")
    public List<VitalSignResponse> getVitalSignHistoryByPatientId(
            @PathVariable Long patientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        return vitalSignService.getVitalSignHistoryByPatientId(patientId, from, to);
    }
}