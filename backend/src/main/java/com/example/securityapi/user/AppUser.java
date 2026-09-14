package com.example.securityapi.user;

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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "app_users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_email",
                        columnNames = "email"
                )
        }
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "email",
            nullable = false,
            length = 320
    )
    private String email;

    @Column(
            name = "password_hash",
            nullable = false
    )
    private String passwordHash;

    /*
     * Keep this field for backward compatibility.
     *
     * If you later migrate to app_roles and app_user_roles,
     * remove this field only after security-api reads roles
     * from those relationship tables.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 20
    )
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "account_status",
            nullable = false,
            length = 30
    )
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(
            name = "email_verified",
            nullable = false
    )
    private boolean emailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    /**
     * Required by JPA.
     */
    protected AppUser() {
    }

    /**
     * Backward-compatible constructor.
     */
    public AppUser(
            String name,
            String email,
            String passwordHash,
            Role role
    ) {
        this(
                name,
                email,
                passwordHash,
                role,
                AccountStatus.ACTIVE
        );
    }

    /**
     * Constructor supporting account status.
     */
    public AppUser(
            String name,
            String email,
            String passwordHash,
            Role role,
            AccountStatus accountStatus
    ) {
        this.name = requireText(name, "name");
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(
                passwordHash,
                "passwordHash"
        );
        this.role = Objects.requireNonNullElse(
                role,
                Role.USER
        );
        this.accountStatus = Objects.requireNonNullElse(
                accountStatus,
                AccountStatus.ACTIVE
        );
        this.emailVerified = false;
    }

    /**
     * Sets timestamps before the first INSERT.
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (role == null) {
            role = Role.USER;
        }

        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }

        email = normalizeEmail(email);
        name = requireText(name, "name");
    }

    /**
     * Updates the timestamp before every UPDATE.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        email = normalizeEmail(email);
        name = requireText(name, "name");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateName(String name) {
        this.name = requireText(name, "name");
    }

    public void updateEmail(String email) {
        this.email = normalizeEmail(email);
        this.emailVerified = false;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = requireText(
                passwordHash,
                "passwordHash"
        );
    }

    public void changeRole(Role role) {
        this.role = Objects.requireNonNull(
                role,
                "role must not be null"
        );
    }

    public void changeAccountStatus(
            AccountStatus accountStatus
    ) {
        this.accountStatus = Objects.requireNonNull(
                accountStatus,
                "accountStatus must not be null"
        );
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void markEmailUnverified() {
        this.emailVerified = false;
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
    }

    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    public boolean isLocked() {
        return accountStatus == AccountStatus.LOCKED;
    }

    public boolean isDisabled() {
        return accountStatus == AccountStatus.DISABLED;
    }

    private static String normalizeEmail(String email) {
        String value = requireText(email, "email");

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }
}