package com.brandPitara.sfs.provider.controller;

import com.brandPitara.sfs.provider.dto.ProviderProfileResponse;
import com.brandPitara.sfs.provider.dto.ProviderProfileUpsertRequest;
import com.brandPitara.sfs.provider.service.ProviderProfileService;
import com.brandPitara.sfs.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

  private final ProviderProfileService providerProfileService;
  private final CurrentUserService currentUserService;

  // Provider creates/updates their profile
  @PostMapping("/me/profile")
  @PreAuthorize("hasAnyRole('WORKER','BRAND')")
  public ProviderProfileResponse upsertMyProfile(@Valid @RequestBody ProviderProfileUpsertRequest request) {
    return providerProfileService.onboardOrUpdateMyProfile(currentUserService.requireUserId(), request);
  }

  @GetMapping("/me/profile")
  @PreAuthorize("hasAnyRole('WORKER','BRAND')")
  public ProviderProfileResponse getMyProfile() {
    return providerProfileService.getMyProfile(currentUserService.requireUserId());
  }

  // Public profile for customers
  @GetMapping("/{providerId}")
  public ProviderProfileResponse getPublic(@PathVariable Long providerId) {
    return providerProfileService.getPublicProfile(providerId);
  }

  // Similar providers (public)
  @GetMapping("/{providerId}/similar")
  public List<ProviderProfileResponse> similar(@PathVariable Long providerId,
                                               @RequestParam(defaultValue = "10") int limit) {
    return providerProfileService.getSimilarProviders(providerId, Math.min(limit, 20));
  }
}
