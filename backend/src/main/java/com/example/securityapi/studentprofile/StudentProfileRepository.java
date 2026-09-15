package com.example.securityapi.studentprofile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository
        extends JpaRepository<StudentProfile, Long> {
}
