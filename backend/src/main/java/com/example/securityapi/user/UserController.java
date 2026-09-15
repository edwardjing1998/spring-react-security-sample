package com.example.securityapi.user;

import com.example.securityapi.auth.AuthDtos.UserResponse;
import com.example.securityapi.auth.AuthService;
import com.example.securityapi.membership.MembershipType;
import com.example.securityapi.membership.SchoolMembershipRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository users;
    private final SchoolMembershipRepository memberships;

    public UserController(
            UserRepository users,
            SchoolMembershipRepository memberships
    ) {
        this.users = users;
        this.memberships = memberships;
    }

    @GetMapping("/users/me")
    public UserResponse me(
            Authentication authentication
    ) {
        AppUser user = users
                .findByEmailIgnoreCase(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return AuthService.toResponse(user);
    }

    @GetMapping("/admin/summary")
    public Map<String, Object> adminSummary() {
        return Map.of(
                "message",
                "You are authorized as an administrator.",
                "registeredUsers",
                users.count()
        );
    }

    @GetMapping("/admin/users")
    public List<UserResponse> users() {
        return users.findAll()
                .stream()
                .map(AuthService::toResponse)
                .sorted((left, right) ->
                        left.email().compareToIgnoreCase(
                                right.email()
                        )
                )
                .toList();
    }

    @GetMapping("/student-profile-users")
    public List<UserResponse> studentProfileUsers() {
        Set<Long> studentUserIds =
                memberships
                        .findByMembershipTypeAndMembershipStatusIgnoreCaseOrderByUserId(
                                MembershipType.STUDENT,
                                "ACTIVE"
                        )
                        .stream()
                        .map(membership ->
                                membership.getUserId()
                        )
                        .collect(Collectors.toSet());

        return users.findAllById(studentUserIds)
                .stream()
                .map(AuthService::toResponse)
                .sorted((left, right) ->
                        left.email().compareToIgnoreCase(
                                right.email()
                        )
                )
                .toList();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status",
                "UP"
        );
    }
}