package com.brandPitara.sfs.dashboard.audit.dto;

import com.brandPitara.sfs.dashboard.audit.entity.DashboardActionAuditEntity;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class DashboardActionAuditEntryDto {

    private Long id;
    private Long dashboardUserId;
    private String dashboardUserName;
    private DashboardRole dashboardUserRole;
    private DashboardAuditAction action;
    private ReviewEntityType entityType;
    private Long entityId;
    private Long projectId;
    private String summary;
    private String ipAddress;
    private String userAgent;
    private OffsetDateTime createdAt;
    private OffsetDateTime performedAt;
    private Long performedByUserId;
    private String performedByUserName;
    private DashboardRole performedByUserRole;

    public static DashboardActionAuditEntryDto from(DashboardActionAuditEntity e) {
        return DashboardActionAuditEntryDto.builder()
                .id(e.getId())
                .dashboardUserId(e.getDashboardUserId())
                .dashboardUserName(e.getDashboardUserName())
                .dashboardUserRole(e.getDashboardUserRole())
                .action(e.getAction())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .projectId(e.getProjectId())
                .summary(e.getSummary())
                .ipAddress(e.getIpAddress())
                .userAgent(e.getUserAgent())
                .createdAt(e.getCreatedAt())
                .performedAt(e.getCreatedAt())
                .performedByUserId(e.getDashboardUserId())
                .performedByUserName(e.getDashboardUserName())
                .performedByUserRole(e.getDashboardUserRole())
                .build();
    }
}
