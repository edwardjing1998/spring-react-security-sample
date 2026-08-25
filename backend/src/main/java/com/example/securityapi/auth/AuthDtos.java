package com.example.securityapi.auth;

import com.example.securityapi.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {}

    public record SignupRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 72)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                    message = "must include uppercase, lowercase, and a number")
            String password) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record UserResponse(Long id, String name, String email, Role role, Instant createdAt) {}

    public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {}
}

