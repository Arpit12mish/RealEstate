package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectMasterPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/projects/{projectId}/master-plan")
@RequiredArgsConstructor
public class DashboardProjectMasterPlanController {

  private final ProjectMasterPlanService projectMasterPlanService;
  private final DashboardActionAuditService dashboardActionAuditService;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
  public ProjectMasterPlanResponse get(@PathVariable Long projectId) {
    return projectMasterPlanService.adminGet(projectId);
  }

  @PutMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
  public ProjectMasterPlanResponse upsert(
      @PathVariable Long projectId,
      @Valid @RequestBody ProjectMasterPlanUpsertRequest request
  ) {
    ProjectMasterPlanResponse response = projectMasterPlanService.upsert(projectId, request);
    dashboardActionAuditService.record(
        DashboardAuditAction.PROJECT_MASTER_PLAN_UPSERTED,
        ReviewEntityType.PROJECT_MASTER_PLAN,
        response.getId(),
        projectId
    );
    return response;
  }

  @PatchMapping("/active")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectMasterPlanResponse setActive(
      @PathVariable Long projectId,
      @RequestParam boolean active
  ) {
    ProjectMasterPlanResponse response = projectMasterPlanService.setActive(projectId, active);
    dashboardActionAuditService.record(
        DashboardAuditAction.PROJECT_MASTER_PLAN_ACTIVATED,
        ReviewEntityType.PROJECT_MASTER_PLAN,
        response.getId(),
        projectId
    );
    return response;
  }

  @DeleteMapping
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long projectId) {
    ProjectMasterPlanResponse existing = projectMasterPlanService.adminGet(projectId);
    projectMasterPlanService.softDelete(projectId);
    dashboardActionAuditService.record(
        DashboardAuditAction.PROJECT_MASTER_PLAN_DELETED,
        ReviewEntityType.PROJECT_MASTER_PLAN,
        existing != null ? existing.getId() : projectId,
        projectId
    );
  }
}
