package com.iothealth.backend.service;

import com.iothealth.backend.dto.patient.PatientRequest;
import com.iothealth.backend.dto.patient.PatientResponse;
import com.iothealth.backend.entity.Gender;
import com.iothealth.backend.entity.Patient;
import com.iothealth.backend.exception.BadRequestException;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.repository.PatientRepository;
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
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient patient;
    private PatientRequest request;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(1L)
                .firstName("Sara")
                .lastName("El Amrani")
                .age(42)
                .gender(Gender.FEMALE)
                .roomNumber("A-102")
                .medicalCondition("Cardiac observation")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        request = new PatientRequest(
                "Sara", "El Amrani", 42, Gender.FEMALE, "A-102", "Cardiac observation"
        );
    }

    // --- createPatient ---

    @Test
    void createPatient_success() {
        when(patientRepository.existsByRoomNumber("A-102")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        PatientResponse response = patientService.createPatient(request);

        assertThat(response.firstName()).isEqualTo("Sara");
        assertThat(response.roomNumber()).isEqualTo("A-102");
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void createPatient_duplicateRoomNumber_throwsBadRequest() {
        when(patientRepository.existsByRoomNumber("A-102")).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("A-102");

        verify(patientRepository, never()).save(any());
    }

    // --- getAllPatients ---

    @Test
    void getAllPatients_returnsList() {
        when(patientRepository.findAll()).thenReturn(List.of(patient));

        List<PatientResponse> result = patientService.getAllPatients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).lastName()).isEqualTo("El Amrani");
    }

    @Test
    void getAllPatients_empty_returnsEmptyList() {
        when(patientRepository.findAll()).thenReturn(List.of());

        assertThat(patientService.getAllPatients()).isEmpty();
    }

    // --- getPatientById ---

    @Test
    void getPatientById_found_returnsResponse() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        PatientResponse response = patientService.getPatientById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getPatientById_notFound_throwsResourceNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- updatePatient ---

    @Test
    void updatePatient_sameRoomNumber_success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        PatientResponse response = patientService.updatePatient(1L, request);

        assertThat(response.firstName()).isEqualTo("Sara");
        verify(patientRepository, never()).existsByRoomNumber(any());
    }

    @Test
    void updatePatient_newRoomNumber_alreadyTaken_throwsBadRequest() {
        PatientRequest newRequest = new PatientRequest(
                "Sara", "El Amrani", 42, Gender.FEMALE, "B-201", "Cardiac observation"
        );
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.existsByRoomNumber("B-201")).thenReturn(true);

        assertThatThrownBy(() -> patientService.updatePatient(1L, newRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("B-201");
    }

    // --- deletePatient ---

    @Test
    void deletePatient_success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        patientService.deletePatient(1L);

        verify(patientRepository).delete(patient);
    }

    @Test
    void deletePatient_notFound_throwsResourceNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.deletePatient(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}