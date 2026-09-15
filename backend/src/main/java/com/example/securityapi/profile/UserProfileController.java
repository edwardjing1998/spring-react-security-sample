package com.example.securityapi.profile;

import com.example.securityapi.profile.UserProfileDtos.CreateProfileRequest;
import com.example.securityapi.profile.UserProfileDtos.ProfileResponse;
import com.example.securityapi.profile.UserProfileDtos.UpdateProfileRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/user-profiles")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProfileResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{userId}")
    public ProfileResponse findOne(@PathVariable Long userId) {
        return service.findByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse create(@Valid @RequestBody CreateProfileRequest request) {
        return service.create(request);
    }

    @PutMapping("/{userId}")
    public ProfileResponse update(@PathVariable Long userId,
                                  @Valid @RequestBody UpdateProfileRequest request) {
        return service.update(userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        service.delete(userId);
    }
}
