package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.project.dto.ProjectHighlightResponse;
import com.brandPitara.sfs.project.dto.ProjectHighlightUpsertRequest;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.project.service.ProjectHighlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/projects")
@RequiredArgsConstructor
public class DashboardProjectHighlightController {

    private final ProjectHighlightService projectHighlightService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    /**
     * Add project highlight.
     */
    @PostMapping("/{projectId}/highlights")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectHighlightResponse add(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectHighlightUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectHighlightResponse response = projectHighlightService.addHighlight(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.HIGHLIGHT_CREATED, ReviewEntityType.PROJECT_HIGHLIGHT, response.getId(), projectId);
        return response;
    }

    /**
     * Update project highlight.
     */
    @PutMapping("/{projectId}/highlights/{highlightId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectHighlightResponse update(
            @PathVariable Long projectId,
            @PathVariable Long highlightId,
            @Valid @RequestBody ProjectHighlightUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectHighlightResponse response = projectHighlightService.updateHighlight(projectId, highlightId, request);
        dashboardActionAuditService.record(DashboardAuditAction.HIGHLIGHT_UPDATED, ReviewEntityType.PROJECT_HIGHLIGHT, response.getId(), projectId);
        return response;
    }

    /**
     * List dashboard highlights.
     */
    @GetMapping("/{projectId}/highlights")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectHighlightResponse> list(@PathVariable Long projectId) {
        return projectHighlightService.adminList(projectId);
    }

    /**
     * Only ADMIN can delete for now.
     */
    @DeleteMapping("/{projectId}/highlights/{highlightId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable Long projectId,
            @PathVariable Long highlightId
    ) {
        projectHighlightService.softDelete(projectId, highlightId);
        dashboardActionAuditService.record(DashboardAuditAction.HIGHLIGHT_DELETED, ReviewEntityType.PROJECT_HIGHLIGHT, highlightId, projectId);
    }
}