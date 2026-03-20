package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.dto.ProjectHighlightResponse;
import com.brandPitara.sfs.project.service.ProjectHighlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectHighlightPublicController {

  private final ProjectHighlightService projectHighlightService;

  @GetMapping("/{projectId}/highlights")
  public List<ProjectHighlightResponse> list(@PathVariable Long projectId) {
    return projectHighlightService.publicList(projectId);
  }
}