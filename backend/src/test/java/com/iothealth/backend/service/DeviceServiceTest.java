package com.iothealth.backend.service;

import com.iothealth.backend.dto.device.DeviceRequest;
import com.iothealth.backend.dto.device.DeviceResponse;
import com.iothealth.backend.entity.Device;
import com.iothealth.backend.entity.DeviceStatus;
import com.iothealth.backend.entity.Gender;
import com.iothealth.backend.entity.Patient;
import com.iothealth.backend.exception.BadRequestException;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private PatientService patientService;

    @InjectMocks
    private DeviceService deviceService;

    private Patient patient;
    private Device device;
    private DeviceRequest request;

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

        request = new DeviceRequest("DEV-001", "Multi-parameter monitor", DeviceStatus.ACTIVE, 1L);
    }

    // --- createDevice ---

    @Test
    void createDevice_success() {
        when(deviceRepository.existsByDeviceCode("DEV-001")).thenReturn(false);
        when(deviceRepository.existsByPatientId(1L)).thenReturn(false);
        when(patientService.findPatientEntityById(1L)).thenReturn(patient);
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        DeviceResponse response = deviceService.createDevice(request);

        assertThat(response.deviceCode()).isEqualTo("DEV-001");
        assertThat(response.status()).isEqualTo(DeviceStatus.ACTIVE);
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void createDevice_duplicateDeviceCode_throwsBadRequest() {
        when(deviceRepository.existsByDeviceCode("DEV-001")).thenReturn(true);

        assertThatThrownBy(() -> deviceService.createDevice(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DEV-001");

        verify(deviceRepository, never()).save(any());
    }

    @Test
    void createDevice_patientAlreadyHasDevice_throwsBadRequest() {
        when(deviceRepository.existsByDeviceCode("DEV-001")).thenReturn(false);
        when(deviceRepository.existsByPatientId(1L)).thenReturn(true);

        assertThatThrownBy(() -> deviceService.createDevice(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("patientId=1");

        verify(deviceRepository, never()).save(any());
    }

    // --- getAllDevices ---

    @Test
    void getAllDevices_returnsList() {
        when(deviceRepository.findAll()).thenReturn(List.of(device));

        List<DeviceResponse> result = deviceService.getAllDevices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deviceCode()).isEqualTo("DEV-001");
    }

    // --- getDeviceById ---

    @Test
    void getDeviceById_found_returnsResponse() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        DeviceResponse response = deviceService.getDeviceById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getDeviceById_notFound_throwsResourceNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- getDeviceByCode ---

    @Test
    void getDeviceByCode_found_returnsResponse() {
        when(deviceRepository.findByDeviceCode("DEV-001")).thenReturn(Optional.of(device));

        DeviceResponse response = deviceService.getDeviceByCode("DEV-001");

        assertThat(response.deviceCode()).isEqualTo("DEV-001");
    }

    @Test
    void getDeviceByCode_notFound_throwsResourceNotFound() {
        when(deviceRepository.findByDeviceCode("DEV-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceByCode("DEV-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("DEV-999");
    }

    // --- getDeviceByPatientId ---

    @Test
    void getDeviceByPatientId_found_returnsResponse() {
        when(deviceRepository.findByPatientId(1L)).thenReturn(Optional.of(device));

        DeviceResponse response = deviceService.getDeviceByPatientId(1L);

        assertThat(response.patientId()).isEqualTo(1L);
    }

    @Test
    void getDeviceByPatientId_notFound_throwsResourceNotFound() {
        when(deviceRepository.findByPatientId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.getDeviceByPatientId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- deleteDevice ---

    @Test
    void deleteDevice_success() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));

        deviceService.deleteDevice(1L);

        verify(deviceRepository).delete(device);
    }

    @Test
    void deleteDevice_notFound_throwsResourceNotFound() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deviceService.deleteDevice(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}