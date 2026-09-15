package com.example.securityapi.teacherprofile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "employee_number", length = 100)
    private String employeeNumber;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(name = "department_name", length = 150)
    private String departmentName;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TeacherProfile() {
    }

    public TeacherProfile(Long userId,
                          String employeeNumber,
                          String jobTitle,
                          String departmentName,
                          String biography) {
        this.userId = userId;
        update(employeeNumber, jobTitle, departmentName, biography);
    }

    public void update(String employeeNumber,
                       String jobTitle,
                       String departmentName,
                       String biography) {
        this.employeeNumber = blankToNull(employeeNumber);
        this.jobTitle = blankToNull(jobTitle);
        this.departmentName = blankToNull(departmentName);
        this.biography = blankToNull(biography);
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
    public String getEmployeeNumber() { return employeeNumber; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartmentName() { return departmentName; }
    public String getBiography() { return biography; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
