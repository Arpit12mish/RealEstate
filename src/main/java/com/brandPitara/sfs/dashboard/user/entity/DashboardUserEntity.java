package com.brandPitara.sfs.dashboard.user.entity;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dashboard_users",
        indexes = {
                @Index(name = "idx_dashboard_users_email", columnList = "email"),
                @Index(name = "idx_dashboard_users_role", columnList = "role"),
                @Index(name = "idx_dashboard_users_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/*
 * Allow-list, not deny-list: only fields explicitly marked @ToString.Include
 * ever appear. A deny-list (@ToString(exclude = ...)) protects only the
 * fields someone remembered to name - any field added later would default
 * to included, which is exactly how "passwordHash" ended up dumpable via
 * Hibernate's own entity-state logging in the first place.
 */
@ToString(onlyExplicitlyIncluded = true)
public class DashboardUserEntity {

    @ToString.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Internal dashboard user name.
     * Example: SFS Admin, SFS Reviewer, SFS Data Entry.
     * Not included in toString(): staff PII.
     */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Dashboard login email.
     * This is separate from mobile app users.
     * Not included in toString(): staff PII.
     */
    @Column(nullable = false, unique = true, length = 180)
    private String email;

    /**
     * BCrypt hashed password.
     * Never store raw password. Not included in toString().
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ToString.Include
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DashboardRole role;

    @ToString.Include
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ToString.Include
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @ToString.Include
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ToString.Include
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.active == null) {
            this.active = true;
        }

        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();

        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    public boolean isActiveUser() {
        return Boolean.TRUE.equals(active);
    }
}