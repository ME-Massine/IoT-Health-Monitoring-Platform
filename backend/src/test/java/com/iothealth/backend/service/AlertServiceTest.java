package com.iothealth.backend.service;

import com.iothealth.backend.dto.alert.AlertResponse;
import com.iothealth.backend.entity.*;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.repository.AlertRepository;
import com.iothealth.backend.websocket.AlertWebSocketPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertWebSocketPublisher alertWebSocketPublisher;

    @InjectMocks
    private AlertService alertService;

    private Patient patient;
    private Device device;
    private VitalSign normalVitalSign;

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

        // Normal readings — should produce no alerts
        normalVitalSign = VitalSign.builder()
                .id(1L)
                .patient(patient)
                .device(device)
                .heartRate(75)
                .temperature(BigDecimal.valueOf(36.8))
                .spo2(98)
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    // helper
    private VitalSign vitalSignWith(Integer hr, BigDecimal temp, Integer spo2) {
        return VitalSign.builder()
                .id(2L)
                .patient(patient)
                .device(device)
                .heartRate(hr)
                .temperature(temp)
                .spo2(spo2)
                .recordedAt(Instant.now())
                .createdAt(Instant.now())
                .build();
    }

    // --- detectAndCreateAlerts: no alerts ---

    @Test
    void detectAndCreateAlerts_normalReadings_noAlertsCreated() {
        List<Alert> result = alertService.detectAndCreateAlerts(normalVitalSign);

        assertThat(result).isEmpty();
        verify(alertRepository, never()).saveAll(any());
    }

    // --- Heart rate thresholds ---

    @Test
    void detectAndCreateAlerts_heartRateCriticalLow_createsCriticalAlert() {
        VitalSign vs = vitalSignWith(45, BigDecimal.valueOf(36.8), 98); // < 50
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.LOW_HEART_RATE);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void detectAndCreateAlerts_heartRateWarningHigh_createsWarningAlert() {
        VitalSign vs = vitalSignWith(115, BigDecimal.valueOf(36.8), 98); // >= 110, <= 120
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.HIGH_HEART_RATE);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void detectAndCreateAlerts_heartRateCriticalHigh_createsCriticalAlert() {
        VitalSign vs = vitalSignWith(125, BigDecimal.valueOf(36.8), 98); // > 120
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.HIGH_HEART_RATE);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    // --- Temperature thresholds ---

    @Test
    void detectAndCreateAlerts_temperatureCriticalLow_createsCriticalAlert() {
        VitalSign vs = vitalSignWith(75, BigDecimal.valueOf(34.5), 98); // < 35.0
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.LOW_TEMPERATURE);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void detectAndCreateAlerts_temperatureWarningHigh_createsWarningAlert() {
        VitalSign vs = vitalSignWith(75, BigDecimal.valueOf(37.9), 98); // >= 37.8, <= 38.0
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.HIGH_TEMPERATURE);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void detectAndCreateAlerts_temperatureCriticalHigh_createsCriticalAlert() {
        VitalSign vs = vitalSignWith(75, BigDecimal.valueOf(38.5), 98); // > 38.0
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.HIGH_TEMPERATURE);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    // --- SpO2 thresholds ---

    @Test
    void detectAndCreateAlerts_spo2Warning_createsWarningAlert() {
        VitalSign vs = vitalSignWith(75, BigDecimal.valueOf(36.8), 93); // <= 94, > 92
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.LOW_SPO2);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.WARNING);
    }

    @Test
    void detectAndCreateAlerts_spo2Critical_createsCriticalAlert() {
        VitalSign vs = vitalSignWith(75, BigDecimal.valueOf(36.8), 90); // <= 92
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.LOW_SPO2);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void detectAndCreateAlerts_spo2ExactlyAtCriticalBoundary_createsCriticalAlert() {
        VitalSign vs = vitalSignWith(75, BigDecimal.valueOf(36.8), 92); // exactly 92 = CRITICAL
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    // --- Multiple alerts in one reading ---

    @Test
    void detectAndCreateAlerts_multipleViolations_createsMultipleAlerts() {
        // High HR critical + low SpO2 critical
        VitalSign vs = vitalSignWith(130, BigDecimal.valueOf(36.8), 88);
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Alert> alerts = alertService.detectAndCreateAlerts(vs);

        assertThat(alerts).hasSize(2);
        assertThat(alerts).extracting(Alert::getType)
                .containsExactlyInAnyOrder(AlertType.HIGH_HEART_RATE, AlertType.LOW_SPO2);
    }

    // --- WebSocket publishing ---

    @Test
    void detectAndCreateAlerts_alertCreated_publishesViaWebSocket() {
        VitalSign vs = vitalSignWith(45, BigDecimal.valueOf(36.8), 98);
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        alertService.detectAndCreateAlerts(vs);

        verify(alertWebSocketPublisher, atLeastOnce()).publishAlert(any(AlertResponse.class));
    }

    // --- getAllAlerts ---

    @Test
    void getAllAlerts_returnsSortedList() {
        Alert alert = Alert.builder()
                .id(1L).type(AlertType.HIGH_HEART_RATE).severity(AlertSeverity.WARNING)
                .message("Warning").resolved(false).patient(patient).device(device)
                .vitalSign(normalVitalSign).createdAt(Instant.now()).build();

        when(alertRepository.findAll(any(Sort.class))).thenReturn(List.of(alert));

        List<AlertResponse> result = alertService.getAllAlerts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(AlertType.HIGH_HEART_RATE);
    }

    // --- resolveAlert ---

    @Test
    void resolveAlert_success() {
        Alert alert = Alert.builder()
                .id(1L).type(AlertType.LOW_SPO2).severity(AlertSeverity.CRITICAL)
                .message("Critical").resolved(false).patient(patient).device(device)
                .vitalSign(normalVitalSign).createdAt(Instant.now()).build();

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);

        AlertResponse response = alertService.resolveAlert(1L);

        assertThat(response.resolved()).isTrue();
        verify(alertRepository).save(alert);
    }

    @Test
    void resolveAlert_notFound_throwsResourceNotFound() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.resolveAlert(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}