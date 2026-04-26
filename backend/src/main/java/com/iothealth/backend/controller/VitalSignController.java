package com.iothealth.backend.controller;

import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import com.iothealth.backend.service.VitalSignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
}