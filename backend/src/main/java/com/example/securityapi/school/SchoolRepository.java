package com.example.securityapi.school;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
    boolean existsBySchoolCodeIgnoreCase(String schoolCode);
    boolean existsBySchoolCodeIgnoreCaseAndIdNot(String schoolCode, Long id);
}
