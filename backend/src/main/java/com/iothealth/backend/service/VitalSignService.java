package com.iothealth.backend.service;

import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.VitalSign;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.mapper.VitalSignMapper;
import com.iothealth.backend.repository.DeviceRepository;
import com.iothealth.backend.repository.VitalSignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VitalSignService {

    private final VitalSignRepository vitalSignRepository;
    private final DeviceRepository deviceRepository;

    public VitalSignResponse ingestVitalSign(VitalSignRequest request) {
        Device device = deviceRepository.findByDeviceCode(request.deviceCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not found with code: " + request.deviceCode()
                ));

        VitalSign vitalSign = VitalSignMapper.toEntity(request, device);
        VitalSign savedVitalSign = vitalSignRepository.save(vitalSign);

        return VitalSignMapper.toResponse(savedVitalSign);
    }

    @Transactional(readOnly = true)
    public VitalSignResponse getLatestVitalSignByPatientId(Long patientId) {
        VitalSign vitalSign = vitalSignRepository.findTopByPatientIdOrderByRecordedAtDesc(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No vital sign readings found for patient id: " + patientId
                ));

        return VitalSignMapper.toResponse(vitalSign);
    }
}