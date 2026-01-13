package com.brandPitara.sfs.project.controller.admin;

import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectMediaUpsertRequest;
import com.brandPitara.sfs.project.service.ProjectMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectMediaController {

  private final ProjectMediaService projectMediaService;

  @PostMapping("/{projectId}/media")
  @PreAuthorize("hasRole('ADMIN')")
  public ProjectMediaResponse addMedia(@PathVariable Long projectId, @Valid @RequestBody ProjectMediaUpsertRequest req) {
    return projectMediaService.addMedia(projectId, req);
  }

  @GetMapping("/{projectId}/media")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ProjectMediaResponse> list(@PathVariable Long projectId) {
    return projectMediaService.adminList(projectId);
  }

  @DeleteMapping("/{projectId}/media/{mediaId}")
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long projectId, @PathVariable Long mediaId) {
    projectMediaService.softDelete(projectId, mediaId);
  }
}
