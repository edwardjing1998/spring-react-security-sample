package com.example.securityapi.teacherprofile;

import com.example.securityapi.teacherprofile.TeacherProfileDtos.CreateTeacherProfileRequest;
import com.example.securityapi.teacherprofile.TeacherProfileDtos.TeacherProfileResponse;
import com.example.securityapi.teacherprofile.TeacherProfileDtos.UpdateTeacherProfileRequest;
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
@RequestMapping("/api/teacher-profiles")
public class TeacherProfileController {
    private final TeacherProfileService service;

    public TeacherProfileController(TeacherProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<TeacherProfileResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{userId}")
    public TeacherProfileResponse findOne(@PathVariable Long userId) {
        return service.findOne(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherProfileResponse create(
            @Valid @RequestBody CreateTeacherProfileRequest request) {
        return service.create(request);
    }

    @PutMapping("/{userId}")
    public TeacherProfileResponse update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateTeacherProfileRequest request) {
        return service.update(userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        service.delete(userId);
    }
}
