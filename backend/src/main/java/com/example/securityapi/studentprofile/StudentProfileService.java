package com.example.securityapi.studentprofile;

import com.example.securityapi.membership.MembershipType;
import com.example.securityapi.membership.SchoolMembershipRepository;
import com.example.securityapi.studentprofile.StudentProfileDtos.CreateStudentProfileRequest;
import com.example.securityapi.studentprofile.StudentProfileDtos.StudentProfileResponse;
import com.example.securityapi.studentprofile.StudentProfileDtos.UpdateStudentProfileRequest;
import com.example.securityapi.user.AppUser;
import com.example.securityapi.user.UserRepository;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StudentProfileService {

    private final StudentProfileRepository profiles;
    private final UserRepository users;
    private final SchoolMembershipRepository memberships;

    public StudentProfileService(
            StudentProfileRepository profiles,
            UserRepository users,
            SchoolMembershipRepository memberships
    ) {
        this.profiles = profiles;
        this.users = users;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public List<StudentProfileResponse> findAll() {
        return profiles.findAll()
                .stream()
                .map(this::toResponse)
                .sorted((left, right) ->
                        left.userEmail().compareToIgnoreCase(
                                right.userEmail()
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse findOne(
            Long userId
    ) {
        return toResponse(
                requireProfile(userId)
        );
    }

    @Transactional
    public StudentProfileResponse create(
            CreateStudentProfileRequest request
    ) {
        /*
         * Verify that the selected user has an active
         * STUDENT membership in school_memberships.
         */
        requireStudent(request.userId());

        if (profiles.existsById(request.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A student profile already exists for this user"
            );
        }

        StudentProfile profile =
                new StudentProfile(
                        request.userId(),
                        request.studentNumber(),
                        request.dateOfBirth(),
                        request.expectedGraduationYear()
                );

        return toResponse(
                profiles.save(profile)
        );
    }

    @Transactional
    public StudentProfileResponse update(
            Long userId,
            UpdateStudentProfileRequest request
    ) {
        StudentProfile profile =
                requireProfile(userId);

        /*
         * Prevent updating a profile if the user no longer
         * has an active STUDENT membership.
         */
        requireStudent(userId);

        profile.update(
                request.studentNumber(),
                request.dateOfBirth(),
                request.expectedGraduationYear()
        );

        return toResponse(
                profiles.save(profile)
        );
    }

    @Transactional
    public void delete(
            Long userId
    ) {
        if (!profiles.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Student profile not found"
            );
        }

        profiles.deleteById(userId);
    }

    private StudentProfile requireProfile(
            Long userId
    ) {
        return profiles.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Student profile not found"
                        )
                );
    }

    /*
     * Student status is determined by school_memberships,
     * not by app_users.role.
     */
    private AppUser requireStudent(
            Long userId
    ) {
        AppUser user = users.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        boolean hasActiveStudentMembership =
                memberships
                        .existsByUserIdAndMembershipTypeAndMembershipStatusIgnoreCase(
                                userId,
                                MembershipType.STUDENT,
                                "ACTIVE"
                        );

        if (!hasActiveStudentMembership) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The user does not have an active STUDENT school membership"
            );
        }

        return user;
    }

    private StudentProfileResponse toResponse(
            StudentProfile profile
    ) {
        AppUser user = users
                .findById(profile.getUserId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return new StudentProfileResponse(
                profile.getUserId(),
                user.getName(),
                user.getEmail(),
                profile.getStudentNumber(),
                profile.getDateOfBirth(),
                profile.getExpectedGraduationYear(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}