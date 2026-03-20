package com.brandPitara.sfs.project.controller.admin;

import com.brandPitara.sfs.project.dto.ProjectHighlightResponse;
import com.brandPitara.sfs.project.dto.ProjectHighlightUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectHighlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectHighlightController {

  private final ProjectHighlightService projectHighlightService;

  @PostMapping("/{projectId}/highlights")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectHighlightResponse add(@PathVariable Long projectId,
                                      @Valid @RequestBody ProjectHighlightUpsertRequest request) {
    return projectHighlightService.addHighlight(projectId, request);
  }

  @PutMapping("/{projectId}/highlights/{highlightId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectHighlightResponse update(@PathVariable Long projectId,
                                         @PathVariable Long highlightId,
                                         @Valid @RequestBody ProjectHighlightUpsertRequest request) {
    return projectHighlightService.updateHighlight(projectId, highlightId, request);
  }

  @GetMapping("/{projectId}/highlights")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ProjectHighlightResponse> list(@PathVariable Long projectId) {
    return projectHighlightService.adminList(projectId);
  }

  @DeleteMapping("/{projectId}/highlights/{highlightId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long projectId, @PathVariable Long highlightId) {
    projectHighlightService.softDelete(projectId, highlightId);
  }
}