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
@RequestMapping("/api/admin/projects/{projectId}/floor-plans")
@RequiredArgsConstructor
public class AdminProjectFloorPlanController {

  private final ProjectFloorPlanService projectFloorPlanService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectFloorPlanResponse create(
      @PathVariable Long projectId,
      @Valid @RequestBody ProjectFloorPlanUpsertRequest request
  ) {
    return projectFloorPlanService.create(projectId, request);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<ProjectFloorPlanResponse> adminList(@PathVariable Long projectId) {
    return projectFloorPlanService.adminList(projectId);
  }

  @PutMapping("/{floorPlanId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectFloorPlanResponse update(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId,
      @Valid @RequestBody ProjectFloorPlanUpsertRequest request
  ) {
    return projectFloorPlanService.update(projectId, floorPlanId, request);
  }

  @PatchMapping("/{floorPlanId}/active")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectFloorPlanResponse setActive(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId,
      @RequestParam boolean active
  ) {
    return projectFloorPlanService.setActive(projectId, floorPlanId, active);
  }

  @DeleteMapping("/{floorPlanId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId
  ) {
    projectFloorPlanService.softDelete(projectId, floorPlanId);
  }
}
