package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builders")
@RequiredArgsConstructor
public class BuilderProjectsPublicController {

  private final ProjectService projectService;

  @GetMapping("/{builderId}/projects")
  public Page<ProjectResponse> list(
      @PathVariable Long builderId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50),
        Sort.by("priority").ascending().and(Sort.by("id").descending()));
    return projectService.publicListByBuilder(builderId, pageable);
  }
}
