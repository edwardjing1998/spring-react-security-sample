package com.example.securityapi.membership;

import com.example.securityapi.membership.SchoolMembershipDtos.MembershipRequest;
import com.example.securityapi.membership.SchoolMembershipDtos.MembershipResponse;
import com.example.securityapi.school.School;
import com.example.securityapi.school.SchoolRepository;
import com.example.securityapi.user.AppUser;
import com.example.securityapi.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SchoolMembershipService {
    private final SchoolMembershipRepository memberships;
    private final UserRepository users;
    private final SchoolRepository schools;

    public SchoolMembershipService(SchoolMembershipRepository memberships,
                                   UserRepository users,
                                   SchoolRepository schools) {
        this.memberships = memberships;
        this.users = users;
        this.schools = schools;
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> findAll() {
        return memberships.findAll().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(MembershipResponse::schoolName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(MembershipResponse::userEmail,
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public MembershipResponse findOne(Long id) {
        return toResponse(requireMembership(id));
    }

    @Transactional
    public MembershipResponse create(MembershipRequest request) {
        validate(request, null);
        SchoolMembership membership = new SchoolMembership(
                request.userId(), request.schoolId(), request.membershipType(),
                request.externalPersonId(), request.membershipStatus(),
                request.startDate(), request.endDate());
        return toResponse(memberships.save(membership));
    }

    @Transactional
    public MembershipResponse update(Long id, MembershipRequest request) {
        SchoolMembership membership = requireMembership(id);
        validate(request, id);
        membership.update(
                request.userId(), request.schoolId(), request.membershipType(),
                request.externalPersonId(), request.membershipStatus(),
                request.startDate(), request.endDate());
        return toResponse(memberships.save(membership));
    }

    @Transactional
    public void delete(Long id) {
        memberships.delete(requireMembership(id));
    }

    private void validate(MembershipRequest request, Long excludedId) {
        requireUser(request.userId());
        requireSchool(request.schoolId());

        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "End date must be on or after start date");
        }

        if (memberships.countDuplicates(
                request.userId(), request.schoolId(), request.membershipType(),
                request.startDate(), excludedId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This school membership already exists");
        }
    }

    private SchoolMembership requireMembership(Long id) {
        return memberships.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "School membership not found"));
    }

    private AppUser requireUser(Long id) {
        return users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private School requireSchool(Long id) {
        return schools.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    private MembershipResponse toResponse(SchoolMembership membership) {
        AppUser user = requireUser(membership.getUserId());
        School school = requireSchool(membership.getSchoolId());
        return new MembershipResponse(
                membership.getId(), user.getId(), user.getName(), user.getEmail(),
                school.getId(), school.getSchoolCode(), school.getSchoolName(),
                membership.getMembershipType(), membership.getExternalPersonId(),
                membership.getMembershipStatus(), membership.getStartDate(),
                membership.getEndDate(), membership.getCreatedAt(),
                membership.getUpdatedAt());
    }
}
