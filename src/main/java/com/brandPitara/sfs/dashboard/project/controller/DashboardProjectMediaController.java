package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectMediaUpsertRequest;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.project.service.ProjectMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/projects")
@RequiredArgsConstructor
public class DashboardProjectMediaController {

    private final ProjectMediaService projectMediaService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    /**
     * Add project image/video/brochure.
     *
     * DATA_ENTRY can add media while preparing project data.
     * ADMIN can also add media.
     */
    @PostMapping("/{projectId}/media")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectMediaResponse addMedia(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectMediaUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectMediaResponse response = projectMediaService.addMedia(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.MEDIA_ADDED, ReviewEntityType.PROJECT_MEDIA, response.getId(), projectId);
        return response;
    }

    @PutMapping("/{projectId}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectMediaResponse updateMedia(
            @PathVariable Long projectId,
            @PathVariable Long mediaId,
            @Valid @RequestBody ProjectMediaUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectMediaResponse response = projectMediaService.updateMedia(projectId, mediaId, request);
        dashboardActionAuditService.record(DashboardAuditAction.MEDIA_UPDATED, ReviewEntityType.PROJECT_MEDIA, mediaId, projectId);
        return response;
    }

    /**
     * Dashboard list should show all non-deleted media, including inactive rows.
     */
    @GetMapping("/{projectId}/media")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectMediaResponse> list(@PathVariable Long projectId) {
        return projectMediaService.adminList(projectId);
    }

    /**
     * Only ADMIN can delete media for now.
     *
     * Later, once ownership/reviewStatus is added:
     * DATA_ENTRY can delete only DRAFT/RECHECK project media.
     */
    @DeleteMapping("/{projectId}/media/{mediaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable Long projectId,
            @PathVariable Long mediaId
    ) {
        projectMediaService.softDelete(projectId, mediaId);
        dashboardActionAuditService.record(DashboardAuditAction.MEDIA_DELETED, ReviewEntityType.PROJECT_MEDIA, mediaId, projectId);
    }
}