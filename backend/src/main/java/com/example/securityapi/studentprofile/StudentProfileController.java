package com.example.securityapi.studentprofile;

import com.example.securityapi.studentprofile.StudentProfileDtos.CreateStudentProfileRequest;
import com.example.securityapi.studentprofile.StudentProfileDtos.StudentProfileResponse;
import com.example.securityapi.studentprofile.StudentProfileDtos.UpdateStudentProfileRequest;
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
@RequestMapping("/api/student-profiles")
public class StudentProfileController {
    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<StudentProfileResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{userId}")
    public StudentProfileResponse findOne(@PathVariable Long userId) {
        return service.findOne(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentProfileResponse create(
            @Valid @RequestBody CreateStudentProfileRequest request) {
        return service.create(request);
    }

    @PutMapping("/{userId}")
    public StudentProfileResponse update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStudentProfileRequest request) {
        return service.update(userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        service.delete(userId);
    }
}
