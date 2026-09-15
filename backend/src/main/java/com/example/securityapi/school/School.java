package com.example.securityapi.school;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "schools", uniqueConstraints =
        @UniqueConstraint(name = "schools_school_code_key", columnNames = "school_code"))
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_code", nullable = false, length = 50)
    private String schoolCode;

    @Column(name = "school_name", nullable = false, length = 255)
    private String schoolName;

    @Column(name = "school_type", length = 50)
    private String schoolType;

    @Column(name = "district_name", length = 255)
    private String districtName;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_code", length = 20)
    private String stateCode;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected School() {
    }

    public School(String schoolCode, String schoolName, String schoolType,
                  String districtName, String addressLine1, String city,
                  String stateCode, String postalCode, String countryCode,
                  String status) {
        update(schoolCode, schoolName, schoolType, districtName, addressLine1,
                city, stateCode, postalCode, countryCode, status);
    }

    public void update(String schoolCode, String schoolName, String schoolType,
                       String districtName, String addressLine1, String city,
                       String stateCode, String postalCode, String countryCode,
                       String status) {
        this.schoolCode = required(schoolCode).toUpperCase(Locale.ROOT);
        this.schoolName = required(schoolName);
        this.schoolType = blankToNull(schoolType);
        this.districtName = blankToNull(districtName);
        this.addressLine1 = blankToNull(addressLine1);
        this.city = blankToNull(city);
        this.stateCode = upperOrNull(stateCode);
        this.postalCode = blankToNull(postalCode);
        this.countryCode = defaultUpper(countryCode, "US");
        this.status = defaultUpper(status, "ACTIVE");
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

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required value must not be blank");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upperOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String defaultUpper(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return (normalized == null ? defaultValue : normalized).toUpperCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public String getSchoolCode() { return schoolCode; }
    public String getSchoolName() { return schoolName; }
    public String getSchoolType() { return schoolType; }
    public String getDistrictName() { return districtName; }
    public String getAddressLine1() { return addressLine1; }
    public String getCity() { return city; }
    public String getStateCode() { return stateCode; }
    public String getPostalCode() { return postalCode; }
    public String getCountryCode() { return countryCode; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
