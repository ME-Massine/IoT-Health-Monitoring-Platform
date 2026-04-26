package com.iothealth.backend.service;

import com.iothealth.backend.dto.patient.PatientRequest;
import com.iothealth.backend.dto.patient.PatientResponse;
import com.iothealth.backend.entity.Patient;
import com.iothealth.backend.exception.BadRequestException;
import com.iothealth.backend.exception.ResourceNotFoundException;
import com.iothealth.backend.mapper.PatientMapper;
import com.iothealth.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientResponse createPatient(PatientRequest request) {
        validateRoomNumberIsUnique(request.roomNumber());

        Patient patient = PatientMapper.toEntity(request);
        Patient savedPatient = patientRepository.save(patient);

        return PatientMapper.toResponse(savedPatient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll()
                .stream()
                .map(PatientMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        Patient patient = findPatientEntityById(id);
        return PatientMapper.toResponse(patient);
    }

    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient existingPatient = findPatientEntityById(id);

        if (!existingPatient.getRoomNumber().equals(request.roomNumber())) {
            validateRoomNumberIsUnique(request.roomNumber());
        }

        PatientMapper.updateEntity(existingPatient, request);
        Patient updatedPatient = patientRepository.save(existingPatient);

        return PatientMapper.toResponse(updatedPatient);
    }

    public void deletePatient(Long id) {
        Patient patient = findPatientEntityById(id);
        patientRepository.delete(patient);
    }

    @Transactional(readOnly = true)
    public Patient findPatientEntityById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    private void validateRoomNumberIsUnique(String roomNumber) {
        if (patientRepository.existsByRoomNumber(roomNumber)) {
            throw new BadRequestException("Room number already assigned: " + roomNumber);
        }
    }
}