package com.example.securityapi.studentprofile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class StudentProfileDtos {
    private StudentProfileDtos() {
    }

    public record CreateStudentProfileRequest(
            @NotNull @Positive Long userId,
            @Size(max = 100) String studentNumber,
            @Past LocalDate dateOfBirth,
            @Min(1900) @Max(2200) Integer expectedGraduationYear
    ) {
    }

    public record UpdateStudentProfileRequest(
            @Size(max = 100) String studentNumber,
            @Past LocalDate dateOfBirth,
            @Min(1900) @Max(2200) Integer expectedGraduationYear
    ) {
    }

    public record StudentProfileResponse(
            Long userId,
            String userName,
            String userEmail,
            String studentNumber,
            LocalDate dateOfBirth,
            Integer expectedGraduationYear,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
