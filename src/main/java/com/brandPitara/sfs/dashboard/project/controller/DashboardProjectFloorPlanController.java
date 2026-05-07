package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/projects/{projectId}/floor-plans")
@RequiredArgsConstructor
public class DashboardProjectFloorPlanController {

    private final ProjectFloorPlanService projectFloorPlanService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectFloorPlanResponse create(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectFloorPlanUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectFloorPlanResponse response = projectFloorPlanService.create(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_CREATED, ReviewEntityType.PROJECT_FLOOR_PLAN, response.getId(), projectId);
        return response;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectFloorPlanResponse> list(@PathVariable Long projectId) {
        return projectFloorPlanService.adminList(projectId);
    }

    @PutMapping("/{floorPlanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectFloorPlanResponse update(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @Valid @RequestBody ProjectFloorPlanUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectFloorPlanResponse response = projectFloorPlanService.update(projectId, floorPlanId, request);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_UPDATED, ReviewEntityType.PROJECT_FLOOR_PLAN, floorPlanId, projectId);
        return response;
    }

    @PatchMapping("/{floorPlanId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectFloorPlanResponse setActive(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @RequestParam boolean active
    ) {
        ProjectFloorPlanResponse response = projectFloorPlanService.setActive(projectId, floorPlanId, active);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_ACTIVATED, ReviewEntityType.PROJECT_FLOOR_PLAN, floorPlanId, projectId);
        return response;
    }

    @DeleteMapping("/{floorPlanId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId
    ) {
        projectFloorPlanService.softDelete(projectId, floorPlanId);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_DELETED, ReviewEntityType.PROJECT_FLOOR_PLAN, floorPlanId, projectId);
    }
}
