package com.example.securityapi.user;

import com.example.securityapi.auth.AuthDtos.UserResponse;
import com.example.securityapi.auth.AuthService;
import java.util.Map;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserRepository users;

    public UserController(UserRepository users) { this.users = users; }

    @GetMapping("/users/me")
    public UserResponse me(Authentication authentication) {
        AppUser user = users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return AuthService.toResponse(user);
    }

    @GetMapping("/admin/summary")
    public Map<String, Object> adminSummary() {
        return Map.of("message", "You are authorized as an administrator.", "registeredUsers", users.count());
    }

    @GetMapping("/admin/users")
    public List<UserResponse> users() {
        return users.findAll().stream()
                .map(AuthService::toResponse)
                .sorted((left, right) -> left.email().compareToIgnoreCase(right.email()))
                .toList();
    }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}
