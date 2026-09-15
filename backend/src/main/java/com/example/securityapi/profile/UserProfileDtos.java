package com.example.securityapi.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserProfileDtos {
    private UserProfileDtos() {
    }

    public record CreateProfileRequest(
            @NotNull @Positive Long userId,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 200) String displayName,
            @Size(max = 20) String preferredLanguage,
            @Size(max = 50) String timezone,
            @Size(max = 2048) String avatarUrl
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 200) String displayName,
            @Size(max = 20) String preferredLanguage,
            @Size(max = 50) String timezone,
            @Size(max = 2048) String avatarUrl
    ) {
    }

    public record ProfileResponse(
            Long userId,
            String userEmail,
            String firstName,
            String lastName,
            String displayName,
            String preferredLanguage,
            String timezone,
            String avatarUrl,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
