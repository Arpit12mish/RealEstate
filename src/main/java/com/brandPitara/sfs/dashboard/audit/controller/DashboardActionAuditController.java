package com.brandPitara.sfs.dashboard.audit.controller;

import com.brandPitara.sfs.dashboard.audit.dto.DashboardActionAuditEntryDto;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/dashboard/audit")
@RequiredArgsConstructor
public class DashboardActionAuditController {

    private final DashboardActionAuditService auditService;

    @GetMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<DashboardActionAuditEntryDto> listByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        return auditService.listByProject(projectId, pageable);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<DashboardActionAuditEntryDto> listByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        return auditService.listByUser(userId, pageable);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<DashboardActionAuditEntryDto> listAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(required = false) DashboardAuditAction action,
            @RequestParam(required = false) ReviewEntityType entityType,
            @RequestParam(required = false) DashboardRole userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        return auditService.listAll(fromDate, toDate, action, entityType, userRole, pageable);
    }
}
