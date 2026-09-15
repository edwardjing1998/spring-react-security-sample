package com.example.securityapi.membership;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class SchoolMembershipDtos {
    private SchoolMembershipDtos() {
    }

    public record MembershipRequest(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long schoolId,
            @NotNull MembershipType membershipType,
            @Size(max = 100) String externalPersonId,
            @Size(max = 30) String membershipStatus,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record MembershipResponse(
            Long id,
            Long userId,
            String userName,
            String userEmail,
            Long schoolId,
            String schoolCode,
            String schoolName,
            MembershipType membershipType,
            String externalPersonId,
            String membershipStatus,
            LocalDate startDate,
            LocalDate endDate,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
