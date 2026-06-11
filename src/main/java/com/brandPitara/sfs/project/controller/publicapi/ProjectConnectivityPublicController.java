package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivitySearchResponse;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectConnectivityPublicController {

  private final ProjectConnectivityService projectConnectivityService;

  @GetMapping("/{projectId}/connectivity")
  public ProjectConnectivityResponse get(@PathVariable Long projectId) {
    return projectConnectivityService.publicGet(projectId);
  }

  @GetMapping("/{projectId}/connectivity/search")
  public ProjectConnectivitySearchResponse search(
      @PathVariable Long projectId,
      @RequestParam String query
  ) {
    return projectConnectivityService.searchNearby(projectId, query);
  }
}
