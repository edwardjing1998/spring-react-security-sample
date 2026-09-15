package com.example.securityapi.membership;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolMembershipRepository
        extends JpaRepository<SchoolMembership, Long> {

    boolean existsBySchoolId(Long schoolId);

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