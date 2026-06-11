package com.brandPitara.sfs.project.controller.publicapi;

import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.project.service.ProjectMediaService;
import com.brandPitara.sfs.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectPublicController {

  private final ProjectService projectService;
  private final ProjectMediaService projectMediaService;

  @GetMapping("/{projectId}")
  public ProjectPublicResponse get(@PathVariable Long projectId) {
    return projectService.publicGet(projectId);
  }

  @GetMapping("/{projectId}/media")
  public List<ProjectMediaResponse> media(@PathVariable Long projectId) {
    return projectMediaService.publicList(projectId);
  }

  @GetMapping("/feature")
  public Page<ProjectPublicResponse> featured(
      @RequestParam(required = false) Long builderId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    Pageable pageable = PageRequest.of(
        page,
        Math.min(size, 20),
        Sort.by("priority").ascending().and(Sort.by("id").descending())
    );

    return projectService.publicFeatured(builderId, pageable);
  }

  @GetMapping("/browse")
  public Page<ProjectPublicResponse> browse(
      @RequestParam(required = false) List<UnitConfigurationType> unitConfigurations,
      @RequestParam(required = false) Long cityId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(
        page,
        Math.min(size, 50),
        Sort.by("priority").ascending().and(Sort.by("id").descending())
    );

    return projectService.publicBrowse(unitConfigurations, cityId, pageable);
  }
}
