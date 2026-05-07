package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.project.dto.DashboardProjectWorkspaceResponse;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectWorkspaceService;
import com.brandPitara.sfs.dashboard.validator.DashboardProjectValidator;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardProjectController {

    private final ProjectService projectService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;
    private final DashboardProjectWorkspaceService dashboardProjectWorkspaceService;
    private final DashboardProjectValidator projectValidator;

    /**
     * DATA_ENTRY can create project data.
     * ADMIN can also create project data.
     *
     * Existing ProjectService creates:
     * published = false
     * deleted = false
     *
     * That is safe for dashboard draft flow.
     */
    @PostMapping("/builders/{builderId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectResponse create(
            @PathVariable Long builderId,
            @Valid @RequestBody ProjectUpsertRequest request
    ) {
        projectValidator.validate(request);
        ProjectResponse response = projectService.create(builderId, request);
        dashboardProjectOwnershipService.assignCurrentUserAsCreator(response.getId());
        dashboardActionAuditService.record(DashboardAuditAction.PROJECT_CREATED, ReviewEntityType.PROJECT, response.getId(), response.getId());
        return response;
    }

    /**
     * DATA_ENTRY can update project data.
     * ADMIN can update project data.
     *
     * Later, after review_status is added, DATA_ENTRY will only be allowed
     * to update DRAFT / RECHECK / REJECTED projects.
     */
    @PutMapping("/projects/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectResponse update(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpsertRequest request
    ) {
        projectValidator.validate(request);
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectResponse response = projectService.update(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PROJECT_UPDATED, ReviewEntityType.PROJECT, projectId, projectId);
        return response;
    }

    /**
     * All dashboard roles can view project details.
     */
    @GetMapping("/projects/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public ProjectResponse get(@PathVariable Long projectId) {
        return projectService.adminGet(projectId);
    }

    /**
     * Composed workspace: returns project + all child sections + review state in one call.
     * Replaces 10+ individual fetches on the dashboard project edit/review screen.
     */
    @GetMapping("/projects/{projectId}/workspace")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardProjectWorkspaceResponse getWorkspace(@PathVariable Long projectId) {
        return dashboardProjectWorkspaceService.getWorkspace(projectId);
    }

    /**
     * All dashboard roles can list projects.
     *
     * DATA_ENTRY needs this to see added data.
     * REVIEWER needs this to review pending data.
     * ADMIN needs this for full control.
     */
    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public Page<ProjectResponse> list(
            @RequestParam(required = false) Long builderId,
            @RequestParam(required = false) ReviewStatus reviewStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by("priority").ascending().and(Sort.by("id").descending())
        );

        return projectService.dashboardList(builderId, reviewStatus, pageable);
    }

    /**
     * Temporary ADMIN-only direct publish.
     *
     * Later, this should not be called from normal dashboard UI.
     * Approval workflow should call publish internally after:
     * - no active field issues exist
     * - REVIEWER / ADMIN approves
     */
    @PatchMapping("/projects/{projectId}/published")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse setPublished(
            @PathVariable Long projectId,
            @RequestParam boolean value
    ) {
        ProjectResponse response = projectService.setPublished(projectId, value);
        dashboardActionAuditService.record(
                value ? DashboardAuditAction.PROJECT_PUBLISHED : DashboardAuditAction.PROJECT_UNPUBLISHED,
                ReviewEntityType.PROJECT, projectId, projectId);
        return response;
    }

    /**
     * ADMIN-only active/inactive toggle.
     */
    @PatchMapping("/projects/{projectId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse setActive(
            @PathVariable Long projectId,
            @RequestParam boolean value
    ) {
        ProjectResponse response = projectService.setActive(projectId, value);
        dashboardActionAuditService.record(
                value ? DashboardAuditAction.PROJECT_ACTIVATED : DashboardAuditAction.PROJECT_DEACTIVATED,
                ReviewEntityType.PROJECT, projectId, projectId);
        return response;
    }

    /**
     * Only ADMIN can delete project.
     */
    @DeleteMapping("/projects/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long projectId) {
        projectService.softDelete(projectId);
        dashboardActionAuditService.record(DashboardAuditAction.PROJECT_DELETED, ReviewEntityType.PROJECT, projectId, projectId);
    }

    
}