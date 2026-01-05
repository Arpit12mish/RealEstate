package com.brandPitara.sfs.provider.controller;

import com.brandPitara.sfs.provider.dto.ProviderProjectCreateRequest;
import com.brandPitara.sfs.provider.dto.ProviderProjectResponse;
import com.brandPitara.sfs.provider.service.ProviderProjectService;
import com.brandPitara.sfs.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderProjectController {

  private final ProviderProjectService providerProjectService;
  private final CurrentUserService currentUserService;

  @PostMapping("/me/projects")
  @PreAuthorize("hasAnyRole('WORKER','BRAND')")
  public ProviderProjectResponse createMyProject(@Valid @RequestBody ProviderProjectCreateRequest request) {
    return providerProjectService.createMyProject(currentUserService.requireUserId(), request);
  }

  @GetMapping("/{providerId}/projects")
  public List<ProviderProjectResponse> listProviderProjects(@PathVariable Long providerId) {
    return providerProjectService.listProviderProjects(providerId);
  }

  @DeleteMapping("/me/projects/{projectId}")
  @PreAuthorize("hasAnyRole('WORKER','BRAND')")
  public void deleteMyProject(@PathVariable Long projectId) {
    providerProjectService.deleteMyProject(currentUserService.requireUserId(), projectId);
  }

  @GetMapping("/me/projects")
  @PreAuthorize("hasAnyRole('WORKER','BRAND')")
  public List<ProviderProjectResponse> listMyProjects() {
    return providerProjectService.listMyProjects(currentUserService.requireUserId());
  }

}
