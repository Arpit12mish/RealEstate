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

  // GAP-001: canonical public lookup by backend-owned slug. A literal
  // "slug" segment - not a second {value} pattern sharing this position
  // with {projectId} above - so the two routes differ in path-segment
  // count (2 segments here vs. 1 for /{projectId}) and can never collide,
  // the same way this controller's existing /feature and /browse literal
  // segments already coexist safely with /{projectId} today. Deliberately
  // NOT the same same-position {id:\d+}/{slug:...} regex-disambiguation
  // approach BrandPublicController uses - that pattern exists there
  // because Brand's two lookups share one path position; this one avoids
  // the ambiguity entirely by using a distinct position instead.
  @GetMapping("/slug/{projectSlug}")
  public ProjectPublicResponse getBySlug(@PathVariable String projectSlug) {
    return projectService.publicGetBySlug(projectSlug);
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
