package com.brandPitara.sfs.dashboard.audit.service;

import com.brandPitara.sfs.dashboard.audit.dto.DashboardActionAuditEntryDto;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

public interface DashboardActionAuditService {

    /**
     * Records an action audit entry for the currently authenticated dashboard user.
     * Never throws — failures are logged and silently dropped so they never
     * fail the calling write operation.
     *
     * @param projectId  nullable; set for all project-related entity types
     */
    void record(DashboardAuditAction action, ReviewEntityType entityType, Long entityId, Long projectId);

    Page<DashboardActionAuditEntryDto> listByProject(Long projectId, Pageable pageable);

    Page<DashboardActionAuditEntryDto> listByUser(Long dashboardUserId, Pageable pageable);

    Page<DashboardActionAuditEntryDto> listAll(
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            DashboardAuditAction action,
            ReviewEntityType entityType,
            DashboardRole userRole,
            Pageable pageable
    );
}
