package com.example.securityapi.studentprofile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "student_number", length = 100)
    private String studentNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "expected_graduation_year")
    private Integer expectedGraduationYear;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentProfile() {
    }

    public StudentProfile(Long userId,
                          String studentNumber,
                          LocalDate dateOfBirth,
                          Integer expectedGraduationYear) {
        this.userId = userId;
        update(studentNumber, dateOfBirth, expectedGraduationYear);
    }

    public void update(String studentNumber,
                       LocalDate dateOfBirth,
                       Integer expectedGraduationYear) {
        this.studentNumber = blankToNull(studentNumber);
        this.dateOfBirth = dateOfBirth;
        this.expectedGraduationYear = expectedGraduationYear;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getUserId() { return userId; }
    public String getStudentNumber() { return studentNumber; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Integer getExpectedGraduationYear() { return expectedGraduationYear; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
