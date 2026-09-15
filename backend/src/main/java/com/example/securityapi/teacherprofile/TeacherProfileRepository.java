package com.example.securityapi.teacherprofile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileRepository
        extends JpaRepository<TeacherProfile, Long> {
}
