package com.brandPitara.sfs.project.controller.admin;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminProjectFloorPlanController {

  private final ProjectFloorPlanService projectFloorPlanService;

  @PostMapping("/api/admin/projects/{projectId}/floor-plans")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectFloorPlanResponse create(
      @PathVariable Long projectId,
      @Valid @RequestBody ProjectFloorPlanUpsertRequest request
  ) {
    return projectFloorPlanService.create(projectId, request);
  }

  @GetMapping("/api/admin/projects/{projectId}/floor-plans")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ProjectFloorPlanResponse> adminList(@PathVariable Long projectId) {
    return projectFloorPlanService.adminList(projectId);
  }

  @PutMapping("/api/admin/project-floor-plans/{floorPlanId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectFloorPlanResponse update(
      @PathVariable Long floorPlanId,
      @Valid @RequestBody ProjectFloorPlanUpsertRequest request
  ) {
    return projectFloorPlanService.update(floorPlanId, request);
  }

  @PatchMapping("/api/admin/project-floor-plans/{floorPlanId}/active")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectFloorPlanResponse setActive(
      @PathVariable Long floorPlanId,
      @RequestParam boolean active
  ) {
    return projectFloorPlanService.setActive(floorPlanId, active);
  }

  @DeleteMapping("/api/admin/project-floor-plans/{floorPlanId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long floorPlanId) {
    projectFloorPlanService.softDelete(floorPlanId);
  }
}