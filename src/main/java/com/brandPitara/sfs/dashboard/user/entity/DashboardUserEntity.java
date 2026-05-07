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
public class DashboardUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Internal dashboard user name.
     * Example: SFS Admin, SFS Reviewer, SFS Data Entry.
     */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Dashboard login email.
     * This is separate from mobile app users.
     */
    @Column(nullable = false, unique = true, length = 180)
    private String email;

    /**
     * BCrypt hashed password.
     * Never store raw password.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DashboardRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

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