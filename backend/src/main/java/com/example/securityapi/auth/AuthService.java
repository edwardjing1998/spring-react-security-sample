package com.example.securityapi.auth;

import com.example.securityapi.auth.AuthDtos.*;
import com.example.securityapi.security.JwtService;
import com.example.securityapi.user.AppUser;
import com.example.securityapi.user.Role;
import com.example.securityapi.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException();
        }
        AppUser user = users.save(new AppUser(
                request.name().trim(), email, passwordEncoder.encode(request.password()), Role.USER));
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        AppUser user = users.findByEmailIgnoreCase(email).orElseThrow();
        return response(user);
    }

    private AuthResponse response(AppUser user) {
        return new AuthResponse(jwtService.createToken(user), "Bearer",
                jwtService.expirationSeconds(), toResponse(user));
    }

    public static UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    public static class EmailAlreadyUsedException extends RuntimeException {}
}

