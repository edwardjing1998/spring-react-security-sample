package com.example.securityapi.membership;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolMembershipRepository
        extends JpaRepository<SchoolMembership, Long> {

    boolean existsBySchoolId(Long schoolId);

    @Query("""
            select count(m) from SchoolMembership m
            where m.userId = :userId
              and m.schoolId = :schoolId
              and m.membershipType = :membershipType
              and ((:startDate is null and m.startDate is null)
                   or m.startDate = :startDate)
              and (:excludedId is null or m.id <> :excludedId)
            """)
    long countDuplicates(@Param("userId") Long userId,
                         @Param("schoolId") Long schoolId,
                         @Param("membershipType") MembershipType membershipType,
                         @Param("startDate") LocalDate startDate,
                         @Param("excludedId") Long excludedId);
}
