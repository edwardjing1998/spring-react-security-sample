package com.example.securityapi.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "school_memberships")
public class SchoolMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false, length = 30)
    private MembershipType membershipType;

    @Column(name = "external_person_id", length = 100)
    private String externalPersonId;

    @Column(name = "membership_status", nullable = false, length = 30)
    private String membershipStatus;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SchoolMembership() {
    }

    public SchoolMembership(Long userId, Long schoolId,
                            MembershipType membershipType,
                            String externalPersonId, String membershipStatus,
                            LocalDate startDate, LocalDate endDate) {
        update(userId, schoolId, membershipType, externalPersonId,
                membershipStatus, startDate, endDate);
    }

    public void update(Long userId, Long schoolId,
                       MembershipType membershipType,
                       String externalPersonId, String membershipStatus,
                       LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.schoolId = schoolId;
        this.membershipType = membershipType;
        this.externalPersonId = blankToNull(externalPersonId);
        this.membershipStatus = defaultUpper(membershipStatus, "ACTIVE");
        this.startDate = startDate;
        this.endDate = endDate;
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

    private static String defaultUpper(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return (normalized == null ? defaultValue : normalized)
                .toUpperCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getSchoolId() { return schoolId; }
    public MembershipType getMembershipType() { return membershipType; }
    public String getExternalPersonId() { return externalPersonId; }
    public String getMembershipStatus() { return membershipStatus; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
