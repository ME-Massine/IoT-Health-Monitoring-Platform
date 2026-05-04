package com.iothealth.backend.service;

import com.iothealth.backend.dto.vitalsign.VitalSignRequest;
import com.iothealth.backend.dto.vitalsign.VitalSignResponse;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.DeviceStatus;
import com.iothealth.backend.entity.Gender;
import com.iothealth.backend.entity.Patient;
import com.iothealth.backend.entity.VitalSign;
import com.iothealth.backend.exception.BadRequestException;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.repository.DeviceRepository;
import com.iothealth.backend.repository.VitalSignRepository;
import com.iothealth.backend.websocket.VitalSignWebSocketPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalSignServiceTest {

    @Mock
    private VitalSignRepository vitalSignRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private VitalSignWebSocketPublisher vitalSignWebSocketPublisher;

    @InjectMocks
    private VitalSignService vitalSignService;

    private Patient patient;
    private Device device;
    private VitalSign vitalSign;
    private VitalSignRequest request;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(1L)
                .firstName("Sara")
                .lastName("El Amrani")
                .age(42)
                .gender(Gender.FEMALE)
                .roomNumber("A-102")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        device = Device.builder()
                .id(1L)
                .deviceCode("DEV-001")
                .type("Multi-parameter monitor")
                .status(DeviceStatus.ACTIVE)
                .patient(patient)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        vitalSign = VitalSign.builder()
                .id(1L)
                .device(device)
                .patient(patient)
                .heartRate(75)
                .temperature(BigDecimal.valueOf(36.8))
                .spo2(98)
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .build();

        request = new VitalSignRequest("DEV-001", 75, BigDecimal.valueOf(36.8), 98, null);
    }

    // --- ingestVitalSign ---

    @Test
    void ingestVitalSign_success() {
        when(deviceRepository.findByDeviceCode("DEV-001")).thenReturn(Optional.of(device));
        when(vitalSignRepository.save(any(VitalSign.class))).thenReturn(vitalSign);
        when(alertService.detectAndCreateAlerts(any(VitalSign.class))).thenReturn(List.of());

        VitalSignResponse response = vitalSignService.ingestVitalSign(request);

        assertThat(response.heartRate()).isEqualTo(75);
        assertThat(response.spo2()).isEqualTo(98);
        verify(vitalSignRepository).save(any(VitalSign.class));
        verify(alertService).detectAndCreateAlerts(any(VitalSign.class));
        verify(vitalSignWebSocketPublisher).publishVitalSign(any(VitalSignResponse.class));
    }

    @Test
    void ingestVitalSign_deviceNotFound_throwsResourceNotFound() {
        when(deviceRepository.findByDeviceCode("DEV-999")).thenReturn(Optional.empty());

        VitalSignRequest badRequest = new VitalSignRequest(
                "DEV-999", 75, BigDecimal.valueOf(36.8), 98, null
        );

        assertThatThrownBy(() -> vitalSignService.ingestVitalSign(badRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DEV-999");

        verify(vitalSignRepository, never()).save(any());
    }

    @Test
    void ingestVitalSign_deviceNotAssignedToPatient_throwsBadRequest() {
        Device unassignedDevice = Device.builder()
                .id(2L)
                .deviceCode("DEV-002")
                .type("Monitor")
                .status(DeviceStatus.ACTIVE)
                .patient(null)  // no patient
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(deviceRepository.findByDeviceCode("DEV-002"))
                .thenReturn(Optional.of(unassignedDevice));

        VitalSignRequest badRequest = new VitalSignRequest(
                "DEV-002", 75, BigDecimal.valueOf(36.8), 98, null
        );

        assertThatThrownBy(() -> vitalSignService.ingestVitalSign(badRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DEV-002");
    }

    // --- getLatestVitalSignByPatientId ---

    @Test
    void getLatestVitalSign_found_returnsResponse() {
        when(vitalSignRepository.findTopByPatientIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(vitalSign));

        VitalSignResponse response = vitalSignService.getLatestVitalSignByPatientId(1L);

        assertThat(response.patientId()).isEqualTo(1L);
        assertThat(response.heartRate()).isEqualTo(75);
    }

    @Test
    void getLatestVitalSign_notFound_throwsResourceNotFound() {
        when(vitalSignRepository.findTopByPatientIdOrderByRecordedAtDesc(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> vitalSignService.getLatestVitalSignByPatientId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- getVitalSignHistoryByPatientId ---

    @Test
    void getVitalSignHistory_noDateRange_returnsList() {
        when(vitalSignRepository.findByPatientIdOrderByRecordedAtDesc(eq(1L), any()))
                .thenReturn(List.of(vitalSign));

        List<VitalSignResponse> result =
                vitalSignService.getVitalSignHistoryByPatientId(1L, null, null, 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void getVitalSignHistory_invalidLimit_throwsBadRequest() {
        assertThatThrownBy(() ->
                vitalSignService.getVitalSignHistoryByPatientId(1L, null, null, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void getVitalSignHistory_onlyFromProvided_throwsBadRequest() {
        Instant from = Instant.now().minusSeconds(3600);

        assertThatThrownBy(() ->
                vitalSignService.getVitalSignHistoryByPatientId(1L, from, null, 10))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getVitalSignHistory_fromAfterTo_throwsBadRequest() {
        Instant now = Instant.now();
        Instant from = now;
        Instant to   = now.minusSeconds(3600);

        assertThatThrownBy(() ->
                vitalSignService.getVitalSignHistoryByPatientId(1L, from, to, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'from'");
    }
}