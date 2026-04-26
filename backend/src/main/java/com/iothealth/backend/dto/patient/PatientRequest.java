package com.iothealth.backend.dto.patient;

import com.iothealth.backend.entity.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatientRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotNull(message = "Age is required")
        @Min(value = 0, message = "Age must be greater than or equal to 0")
        @Max(value = 120, message = "Age must be less than or equal to 120")
        Integer age,

        @NotNull(message = "Gender is required")
        Gender gender,

        @NotBlank(message = "Room number is required")
        @Size(max = 50, message = "Room number must not exceed 50 characters")
        String roomNumber,

        @Size(max = 1000, message = "Medical condition must not exceed 1000 characters")
        String medicalCondition
) {
}