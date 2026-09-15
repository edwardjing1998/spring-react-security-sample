package com.example.securityapi.teacherprofile;

import com.example.securityapi.membership.MembershipType;
import com.example.securityapi.membership.SchoolMembershipRepository;
import com.example.securityapi.teacherprofile.TeacherProfileDtos.CreateTeacherProfileRequest;
import com.example.securityapi.teacherprofile.TeacherProfileDtos.TeacherProfileResponse;
import com.example.securityapi.teacherprofile.TeacherProfileDtos.UpdateTeacherProfileRequest;
import com.example.securityapi.user.AppUser;
import com.example.securityapi.user.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherProfileService {
    private final TeacherProfileRepository profiles;
    private final UserRepository users;
    private final SchoolMembershipRepository memberships;

    public TeacherProfileService(TeacherProfileRepository profiles,
                                 UserRepository users,
                                 SchoolMembershipRepository memberships) {
        this.profiles = profiles;
        this.users = users;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public List<TeacherProfileResponse> findAll() {
        return profiles.findAll().stream()
                .map(this::toResponse)
                .sorted((left, right) ->
                        left.userEmail().compareToIgnoreCase(right.userEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherProfileResponse findOne(Long userId) {
        return toResponse(requireProfile(userId));
    }

    @Transactional
    public TeacherProfileResponse create(CreateTeacherProfileRequest request) {
        requireTeacher(request.userId());

        if (profiles.existsById(request.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A teacher profile already exists for this user");
        }

        TeacherProfile profile = new TeacherProfile(
                request.userId(),
                request.employeeNumber(),
                request.jobTitle(),
                request.departmentName(),
                request.biography());

        return toResponse(profiles.save(profile));
    }

    @Transactional
    public TeacherProfileResponse update(
            Long userId,
            UpdateTeacherProfileRequest request) {
        TeacherProfile profile = requireProfile(userId);
        requireTeacher(userId);
        profile.update(
                request.employeeNumber(),
                request.jobTitle(),
                request.departmentName(),
                request.biography());
        return toResponse(profiles.save(profile));
    }

    @Transactional
    public void delete(Long userId) {
        if (!profiles.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Teacher profile not found");
        }
        profiles.deleteById(userId);
    }

    private TeacherProfile requireProfile(Long userId) {
        return profiles.findById(userId).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher profile not found"));
    }

    private AppUser requireTeacher(Long userId) {
        AppUser user = users.findById(userId).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        boolean hasActiveTeacherMembership = memberships
                .existsByUserIdAndMembershipTypeAndMembershipStatusIgnoreCase(
                        userId,
                        MembershipType.TEACHER,
                        "ACTIVE");

        if (!hasActiveTeacherMembership) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The user does not have an active TEACHER school membership");
        }

        return user;
    }

    private TeacherProfileResponse toResponse(TeacherProfile profile) {
        AppUser user = users.findById(profile.getUserId()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        return new TeacherProfileResponse(
                profile.getUserId(),
                user.getName(),
                user.getEmail(),
                profile.getEmployeeNumber(),
                profile.getJobTitle(),
                profile.getDepartmentName(),
                profile.getBiography(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
