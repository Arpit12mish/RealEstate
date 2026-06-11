package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanInsightDetailResponse;
import com.brandPitara.sfs.project.service.ProjectFloorPlanInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectFloorPlanInsightPublicController {

  private final ProjectFloorPlanInsightService insightService;

  @GetMapping("/{projectId}/floor-plans/{floorPlanId}/insights")
  public ProjectFloorPlanInsightDetailResponse getInsightDetail(
      @PathVariable Long projectId,
      @PathVariable Long floorPlanId
  ) {
    return insightService.publicGetDetail(projectId, floorPlanId);
  }
}
