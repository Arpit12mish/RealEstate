package com.brandPitara.sfs.provider.controller;

import com.brandPitara.sfs.provider.dto.ProviderProfileResponse;
import com.brandPitara.sfs.provider.dto.ProviderProfileUpsertRequest;
import com.brandPitara.sfs.provider.service.ProviderProfileService;
import com.brandPitara.sfs.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

  // Public provider profile endpoints are disabled (legacy — exposed sensitive fields userId/gstNumber).
  // Re-enable only when a ProviderPublicResponse DTO is introduced.
  @GetMapping("/{providerId}")
  public void getPublic(@PathVariable Long providerId) {
    throw new ResponseStatusException(HttpStatus.GONE,
        "Public provider profiles are not available via this API.");
  }

  @GetMapping("/{providerId}/similar")
  public void similar(@PathVariable Long providerId) {
    throw new ResponseStatusException(HttpStatus.GONE,
        "Similar providers endpoint is not available via this API.");
  }
}
