package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionResponse;
import com.brandPitara.sfs.project.dto.FloorPlanRoomDimensionUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectFloorPlanRoomDimensionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms")
@RequiredArgsConstructor
public class DashboardProjectFloorPlanRoomDimensionController {

    private final ProjectFloorPlanRoomDimensionService roomDimensionService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<FloorPlanRoomDimensionResponse> list(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId
    ) {
        return roomDimensionService.list(projectId, floorPlanId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public FloorPlanRoomDimensionResponse create(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @Valid @RequestBody FloorPlanRoomDimensionUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        FloorPlanRoomDimensionResponse response = roomDimensionService.create(projectId, floorPlanId, request);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_UPDATED, ReviewEntityType.PROJECT, floorPlanId, projectId);
        return response;
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public FloorPlanRoomDimensionResponse update(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @PathVariable Long roomId,
            @Valid @RequestBody FloorPlanRoomDimensionUpsertRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        FloorPlanRoomDimensionResponse response = roomDimensionService.update(projectId, floorPlanId, roomId, request);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_UPDATED, ReviewEntityType.PROJECT, floorPlanId, projectId);
        return response;
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable Long projectId,
            @PathVariable Long floorPlanId,
            @PathVariable Long roomId
    ) {
        roomDimensionService.delete(projectId, floorPlanId, roomId);
        dashboardActionAuditService.record(DashboardAuditAction.FLOOR_PLAN_UPDATED, ReviewEntityType.PROJECT, floorPlanId, projectId);
    }
}
