package com.iothealth.backend.service;

import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.DeviceStatus;
import com.iothealth.backend.entity.VitalSign;
import com.iothealth.backend.exception.BadRequestException;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.mapper.VitalSignMapper;
import com.iothealth.backend.repository.DeviceRepository;
import com.iothealth.backend.repository.VitalSignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.iothealth.backend.websocket.VitalSignWebSocketPublisher;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VitalSignService {

    private final VitalSignRepository vitalSignRepository;
    private final DeviceRepository deviceRepository;
    private final AlertService alertService;
    private final VitalSignWebSocketPublisher vitalSignWebSocketPublisher;

    public VitalSignResponse ingestVitalSign(VitalSignRequest request) {
        Device device = deviceRepository.findByDeviceCode(request.deviceCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not found with code: " + request.deviceCode()
                ));

        if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new BadRequestException(
                    "Device " + device.getDeviceCode() + " is currently " + device.getStatus() + " and cannot accept readings"
            );
        }

        VitalSign vitalSign = VitalSignMapper.toEntity(request, device);
        VitalSign savedVitalSign = vitalSignRepository.save(vitalSign);

        alertService.detectAndCreateAlerts(savedVitalSign);

        VitalSignResponse response = VitalSignMapper.toResponse(savedVitalSign);
        vitalSignWebSocketPublisher.publishVitalSign(response);

        return response;
    }

    @Transactional(readOnly = true)
    public VitalSignResponse getLatestVitalSignByPatientId(Long patientId) {
        VitalSign vitalSign = vitalSignRepository.findTopByPatientIdOrderByRecordedAtDesc(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No vital sign readings found for patient id: " + patientId
                ));

        return VitalSignMapper.toResponse(vitalSign);
    }

    @Transactional(readOnly = true)
    public List<VitalSignResponse> getVitalSignHistoryByPatientId(
            Long patientId,
            Instant from,
            Instant to,
            int limit
    ) {
        if ((from == null && to != null) || (from != null && to == null)) {
            throw new BadRequestException("Both 'from' and 'to' timestamps must be provided together");
        }

        if (from != null && from.isAfter(to)) {
            throw new BadRequestException("'from' timestamp must be before 'to' timestamp");
        }

        int sanitizedLimit = sanitizeLimit(limit);
        Pageable pageable = PageRequest.of(0, sanitizedLimit);

        List<VitalSign> vitalSigns;

        if (from != null) {
            vitalSigns = vitalSignRepository.findByPatientIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                    patientId,
                    from,
                    to,
                    pageable
            );
        } else {
            vitalSigns = vitalSignRepository.findByPatientIdOrderByRecordedAtDesc(patientId, pageable);
        }

        return vitalSigns.stream()
                .map(VitalSignMapper::toResponse)
                .toList();
    }

    private int sanitizeLimit(int limit) {
        if (limit < 1) {
            throw new BadRequestException("'limit' must be greater than 0");
        }

        return Math.min(limit, 500);
    }


}