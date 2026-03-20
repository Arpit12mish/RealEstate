package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectFloorPlanPublicController {

  private final ProjectFloorPlanService projectFloorPlanService;

  @GetMapping("/{projectId}/floor-plans")
  public List<ProjectFloorPlanResponse> publicList(@PathVariable Long projectId) {
    return projectFloorPlanService.publicList(projectId);
  }
}