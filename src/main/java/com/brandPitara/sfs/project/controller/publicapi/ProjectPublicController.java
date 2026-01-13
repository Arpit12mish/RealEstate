package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.service.ProjectMediaService;
import com.brandPitara.sfs.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectPublicController {

  private final ProjectService projectService;
  private final ProjectMediaService projectMediaService;

  @GetMapping("/{projectId}")
  public ProjectResponse get(@PathVariable Long projectId) {
    return projectService.publicGet(projectId);
  }

  @GetMapping("/{projectId}/media")
  public List<ProjectMediaResponse> media(@PathVariable Long projectId) {
    return projectMediaService.publicList(projectId);
  }
}
