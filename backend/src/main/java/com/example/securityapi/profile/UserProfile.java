package com.example.securityapi.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "preferred_language", length = 20)
    private String preferredLanguage;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
    }

    public UserProfile(Long userId, String firstName, String lastName,
                       String displayName, String preferredLanguage,
                       String timezone, String avatarUrl) {
        this.userId = userId;
        update(firstName, lastName, displayName, preferredLanguage, timezone, avatarUrl);
    }

    public void update(String firstName, String lastName, String displayName,
                       String preferredLanguage, String timezone, String avatarUrl) {
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.displayName = blankToNull(displayName);
        this.preferredLanguage = defaultIfBlank(preferredLanguage, "en-US");
        this.timezone = defaultIfBlank(timezone, "America/Los_Angeles");
        this.avatarUrl = blankToNull(avatarUrl);
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

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public Long getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDisplayName() { return displayName; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public String getTimezone() { return timezone; }
    public String getAvatarUrl() { return avatarUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
