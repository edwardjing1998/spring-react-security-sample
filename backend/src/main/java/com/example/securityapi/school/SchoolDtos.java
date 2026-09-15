package com.example.securityapi.school;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class SchoolDtos {
    private SchoolDtos() {
    }

    public record SchoolRequest(
            @NotBlank @Size(max = 50) String schoolCode,
            @NotBlank @Size(max = 255) String schoolName,
            @Size(max = 50) String schoolType,
            @Size(max = 255) String districtName,
            @Size(max = 255) String addressLine1,
            @Size(max = 100) String city,
            @Size(max = 20) String stateCode,
            @Size(max = 20) String postalCode,
            @Size(min = 2, max = 2) String countryCode,
            @Size(max = 30) String status
    ) {
    }

    public record SchoolResponse(
            Long id,
            String schoolCode,
            String schoolName,
            String schoolType,
            String districtName,
            String addressLine1,
            String city,
            String stateCode,
            String postalCode,
            String countryCode,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
