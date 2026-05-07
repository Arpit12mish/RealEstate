package com.brandPitara.sfs.dashboard.auth.entity;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dashboard_refresh_tokens",
        indexes = {
                @Index(name = "idx_dashboard_refresh_tokens_user_id", columnList = "dashboard_user_id"),
                @Index(name = "idx_dashboard_refresh_tokens_token_hash", columnList = "token_hash"),
                @Index(name = "idx_dashboard_refresh_tokens_revoked", columnList = "revoked"),
                @Index(name = "idx_dashboard_refresh_tokens_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardRefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Dashboard user who owns this refresh token.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_user_id", nullable = false)
    private DashboardUserEntity dashboardUser;

    /**
     * SHA-256 hash of refresh token.
     * Never store raw refresh token.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    /**
     * Hash of the new token that replaced this token during refresh rotation.
     */
    @Column(name = "replaced_by_token_hash", length = 255)
    private String replacedByTokenHash;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();

        if (this.revoked == null) {
            this.revoked = false;
        }
    }

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return Boolean.TRUE.equals(revoked);
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    public void revoke(String replacedByTokenHash) {
        this.revoked = true;
        this.revokedAt = OffsetDateTime.now();
        this.replacedByTokenHash = replacedByTokenHash;
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = OffsetDateTime.now();
    }
}