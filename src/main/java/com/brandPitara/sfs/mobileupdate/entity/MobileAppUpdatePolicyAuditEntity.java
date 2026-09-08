package com.brandPitara.sfs.mobileupdate.entity;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.PolicyAuditAction;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mobile_app_update_policy_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileAppUpdatePolicyAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MobilePlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PolicyAuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_policy", nullable = false, columnDefinition = "jsonb")
    private JsonNode previousPolicy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_policy", nullable = false, columnDefinition = "jsonb")
    private JsonNode newPolicy;

    @Column(name = "dashboard_user_id", nullable = false)
    private Long dashboardUserId;

    @Column(name = "dashboard_user_name", nullable = false, length = 150)
    private String dashboardUserName;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "change_reason", nullable = false, length = 500)
    private String changeReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}

