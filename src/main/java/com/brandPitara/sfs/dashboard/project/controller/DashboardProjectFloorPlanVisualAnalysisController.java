package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectFloorPlanVisualAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Singular resource - one Visual Analysis block per floor plan. PUT uses
// get-or-create semantics (mirrors how the room/insight CRUD controllers
// work, but without needing a separate POST since there is at most one row).
@RestController
@RequestMapping("/api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/visual-analysis")
@RequiredArgsConstructor
public class DashboardProjectFloorPlanVisualAnalysisController {

    private final ProjectFloorPlanVisualAnalysisService visualAnalysisService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public ProjectFloorPlanVisualAnalysisResponse get(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId
    ) {
        return visualAnalysisService.get(projectId, floorPlanId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectFloorPlanVisualAnalysisResponse upsert(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @Valid @RequestBody ProjectFloorPlanVisualAnalysisUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectFloorPlanVisualAnalysisResponse response = visualAnalysisService.upsert(projectId, floorPlanId, request);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_UPDATED, ReviewEntityType.PROJECT, floorPlanId, projectId);
        return response;
    }
}
