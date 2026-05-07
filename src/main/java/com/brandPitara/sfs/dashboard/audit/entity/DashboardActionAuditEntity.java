package com.brandPitara.sfs.dashboard.audit.entity;

import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dashboard_action_audit",
        indexes = {
                @Index(name = "idx_dashboard_action_audit_user_id", columnList = "dashboard_user_id"),
                @Index(name = "idx_dashboard_action_audit_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_dashboard_action_audit_project_id", columnList = "project_id"),
                @Index(name = "idx_dashboard_action_audit_action", columnList = "action"),
                @Index(name = "idx_dashboard_action_audit_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardActionAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dashboard_user_id")
    private Long dashboardUserId;

    @Column(name = "dashboard_user_name", length = 150)
    private String dashboardUserName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dashboard_user_role", length = 50)
    private DashboardRole dashboardUserRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 80)
    private DashboardAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 80)
    private ReviewEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
