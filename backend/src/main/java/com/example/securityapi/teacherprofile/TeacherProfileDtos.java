package com.example.securityapi.teacherprofile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class TeacherProfileDtos {
    private TeacherProfileDtos() {
    }

    public record CreateTeacherProfileRequest(
            @NotNull @Positive Long userId,
            @Size(max = 100) String employeeNumber,
            @Size(max = 150) String jobTitle,
            @Size(max = 150) String departmentName,
            @Size(max = 10000) String biography
    ) {
    }

    public record UpdateTeacherProfileRequest(
            @Size(max = 100) String employeeNumber,
            @Size(max = 150) String jobTitle,
            @Size(max = 150) String departmentName,
            @Size(max = 10000) String biography
    ) {
    }

    public record TeacherProfileResponse(
            Long userId,
            String userName,
            String userEmail,
            String employeeNumber,
            String jobTitle,
            String departmentName,
            String biography,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
