package com.iothealth.backend.dto.mapper;

import com.iothealth.backend.dto.patient.PatientRequest;
import com.iothealth.backend.dto.patient.PatientResponse;
import com.iothealth.backend.entity.Patient;

public final class PatientMapper {

    private PatientMapper() {
    }

    public static Patient toEntity(PatientRequest request) {
        return Patient.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .age(request.age())
                .gender(request.gender())
                .roomNumber(request.roomNumber())
                .medicalCondition(request.medicalCondition())
                .build();
    }

    public static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getAge(),
                patient.getGender(),
                patient.getRoomNumber(),
                patient.getMedicalCondition(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }

    public static void updateEntity(Patient patient, PatientRequest request) {
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setAge(request.age());
        patient.setGender(request.gender());
        patient.setRoomNumber(request.roomNumber());
        patient.setMedicalCondition(request.medicalCondition());
    }
}