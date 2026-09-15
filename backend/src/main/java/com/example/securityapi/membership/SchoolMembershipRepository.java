package com.example.securityapi.membership;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolMembershipRepository
        extends JpaRepository<SchoolMembership, Long> {

    boolean existsBySchoolId(Long schoolId);

    boolean existsByUserIdAndMembershipTypeAndMembershipStatusIgnoreCase(
            Long userId,
            MembershipType membershipType,
            String membershipStatus
    );

    List<SchoolMembership>
    findByMembershipTypeAndMembershipStatusIgnoreCaseOrderByUserId(
            MembershipType membershipType,
            String membershipStatus
    );

    boolean existsByUserIdAndSchoolIdAndMembershipTypeAndStartDate(
            Long userId,
            Long schoolId,
            MembershipType membershipType,
            LocalDate startDate
    );

    boolean existsByUserIdAndSchoolIdAndMembershipTypeAndStartDateIsNull(
            Long userId,
            Long schoolId,
            MembershipType membershipType
    );

    boolean existsByUserIdAndSchoolIdAndMembershipTypeAndStartDateAndIdNot(
            Long userId,
            Long schoolId,
            MembershipType membershipType,
            LocalDate startDate,
            Long id
    );

    boolean existsByUserIdAndSchoolIdAndMembershipTypeAndStartDateIsNullAndIdNot(
            Long userId,
            Long schoolId,
            MembershipType membershipType,
            Long id
    );
}