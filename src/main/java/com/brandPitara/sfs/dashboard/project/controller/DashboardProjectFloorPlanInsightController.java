package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.project.dto.FloorPlanInsightResponse;
import com.brandPitara.sfs.project.dto.FloorPlanInsightUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectFloorPlanInsightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/insights")
@RequiredArgsConstructor
public class DashboardProjectFloorPlanInsightController {

    private final ProjectFloorPlanInsightService insightService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<FloorPlanInsightResponse> list(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId
    ) {
        return insightService.list(projectId, floorPlanId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public FloorPlanInsightResponse create(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @Valid @RequestBody FloorPlanInsightUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        FloorPlanInsightResponse response = insightService.create(projectId, floorPlanId, request);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_UPDATED, ReviewEntityType.PROJECT, floorPlanId, projectId);
        return response;
    }

    @PutMapping("/{insightId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public FloorPlanInsightResponse update(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @PathVariable Long insightId,
            @Valid @RequestBody FloorPlanInsightUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        FloorPlanInsightResponse response = insightService.update(projectId, floorPlanId, insightId, request);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_UPDATED, ReviewEntityType.PROJECT, floorPlanId, projectId);
        return response;
    }

    @DeleteMapping("/{insightId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @PathVariable Long insightId
    ) {
        insightService.delete(projectId, floorPlanId, insightId);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_UPDATED, ReviewEntityType.PROJECT, floorPlanId, projectId);
    }
}
