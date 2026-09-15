package com.example.securityapi.profile;

import com.example.securityapi.profile.UserProfileDtos.CreateProfileRequest;
import com.example.securityapi.profile.UserProfileDtos.ProfileResponse;
import com.example.securityapi.profile.UserProfileDtos.UpdateProfileRequest;
import com.example.securityapi.user.AppUser;
import com.example.securityapi.user.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
    private final UserProfileRepository profiles;
    private final UserRepository users;

    public UserProfileService(UserProfileRepository profiles, UserRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> findAll() {
        return profiles.findAll().stream()
                .map(this::toResponse)
                .sorted((left, right) -> left.userEmail().compareToIgnoreCase(right.userEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponse findByUserId(Long userId) {
        return toResponse(requireProfile(userId));
    }

    @Transactional
    public ProfileResponse create(CreateProfileRequest request) {
        requireUser(request.userId());
        if (profiles.existsById(request.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A profile already exists for this user");
        }
        UserProfile profile = new UserProfile(
                request.userId(), request.firstName(), request.lastName(),
                request.displayName(), request.preferredLanguage(),
                request.timezone(), request.avatarUrl());
        return toResponse(profiles.save(profile));
    }

    @Transactional
    public ProfileResponse update(Long userId, UpdateProfileRequest request) {
        UserProfile profile = requireProfile(userId);
        profile.update(request.firstName(), request.lastName(), request.displayName(),
                request.preferredLanguage(), request.timezone(), request.avatarUrl());
        return toResponse(profiles.save(profile));
    }

    @Transactional
    public void delete(Long userId) {
        if (!profiles.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found");
        }
        profiles.deleteById(userId);
    }

    private UserProfile requireProfile(Long userId) {
        return profiles.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User profile not found"));
    }

    private AppUser requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileResponse toResponse(UserProfile profile) {
        AppUser user = requireUser(profile.getUserId());
        return new ProfileResponse(
                profile.getUserId(), user.getEmail(), profile.getFirstName(),
                profile.getLastName(), profile.getDisplayName(),
                profile.getPreferredLanguage(), profile.getTimezone(),
                profile.getAvatarUrl(), profile.getCreatedAt(), profile.getUpdatedAt());
    }
}
