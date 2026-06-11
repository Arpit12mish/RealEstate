package com.brandPitara.sfs.project.controller.admin;

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
@RequestMapping("/api/admin/projects/{projectId}/floor-plans/{floorPlanId}/insights")
@RequiredArgsConstructor
public class AdminProjectFloorPlanInsightController {

  private final ProjectFloorPlanInsightService insightService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<FloorPlanInsightResponse> list(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId
  ) {
    return insightService.list(projectId, floorPlanId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public FloorPlanInsightResponse create(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId,
      @Valid @RequestBody FloorPlanInsightUpsertRequest request
  ) {
    return insightService.create(projectId, floorPlanId, request);
  }

  @PutMapping("/{insightId}")
  @PreAuthorize("hasRole('ADMIN')")
  public FloorPlanInsightResponse update(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId,
      @PathVariable Long insightId,
      @Valid @RequestBody FloorPlanInsightUpsertRequest request
  ) {
    return insightService.update(projectId, floorPlanId, insightId, request);
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
  }
}
