package com.brandPitara.sfs.dashboard.audit.entity;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dashboard_login_audit",
        indexes = {
                @Index(name = "idx_dashboard_login_audit_user_id", columnList = "dashboard_user_id"),
                @Index(name = "idx_dashboard_login_audit_email", columnList = "email"),
                @Index(name = "idx_dashboard_login_audit_success", columnList = "success"),
                @Index(name = "idx_dashboard_login_audit_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardLoginAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nullable because failed login can happen before user is found.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_user_id")
    private DashboardUserEntity dashboardUser;

    @Column(length = 180)
    private String email;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();

        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    public static DashboardLoginAuditEntity success(
            DashboardUserEntity user,
            String ipAddress,
            String userAgent
    ) {
        return DashboardLoginAuditEntity.builder()
                .dashboardUser(user)
                .email(user != null ? user.getEmail() : null)
                .success(true)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }

    public static DashboardLoginAuditEntity failure(
            String email,
            String failureReason,
            String ipAddress,
            String userAgent
    ) {
        return DashboardLoginAuditEntity.builder()
                .email(email != null ? email.trim().toLowerCase() : null)
                .success(false)
                .failureReason(failureReason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }
}