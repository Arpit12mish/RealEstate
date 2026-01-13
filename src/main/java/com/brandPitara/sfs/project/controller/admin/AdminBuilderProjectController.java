package com.brandPitara.sfs.project.controller.admin;

import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBuilderProjectController {

  private final ProjectService projectService;

  @PostMapping("/builders/{builderId}/projects")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectResponse create(@PathVariable Long builderId, @Valid @RequestBody ProjectUpsertRequest request) {
    return projectService.create(builderId, request);
  }

  @PutMapping("/projects/{projectId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectResponse update(@PathVariable Long projectId, @RequestBody ProjectUpsertRequest request) {
    return projectService.update(projectId, request);
  }

  @PatchMapping("/projects/{projectId}/published")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectResponse setPublished(@PathVariable Long projectId, @RequestParam boolean value) {
    return projectService.setPublished(projectId, value);
  }

  @PatchMapping("/projects/{projectId}/active")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectResponse setActive(@PathVariable Long projectId, @RequestParam boolean value) {
    return projectService.setActive(projectId, value);
  }

  @DeleteMapping("/projects/{projectId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long projectId) {
    projectService.softDelete(projectId);
  }

  @GetMapping("/projects/{projectId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectResponse get(@PathVariable Long projectId) {
    return projectService.adminGet(projectId);
  }

  @GetMapping("/projects")
  @PreAuthorize("hasRole('ADMIN')")
  public Page<ProjectResponse> list(
      @RequestParam(required = false) Long builderId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable pageable = PageRequest.of(page, Math.min(size, 50),
        Sort.by("priority").ascending().and(Sort.by("id").descending()));
    return projectService.adminList(builderId, pageable);
  }
}
