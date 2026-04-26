package com.iothealth.backend.dto.patient;

import com.iothealth.backend.entity.Gender;

import java.time.Instant;

public record PatientResponse(
        Long id,
        String firstName,
        String lastName,
        Integer age,
        Gender gender,
        String roomNumber,
        String medicalCondition,
        Instant createdAt,
        Instant updatedAt
) {
}