package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.onboarding.ChooseRoleRequest;
import com.brandPitara.sfs.dto.onboarding.SessionResponse;
import com.brandPitara.sfs.service.OnboardingService;
import com.brandPitara.sfs.provider.dto.ProviderProfileResponse;
import com.brandPitara.sfs.provider.dto.ProviderProfileUpsertRequest;
import com.brandPitara.sfs.provider.service.ProviderProfileService;
import com.brandPitara.sfs.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final ProviderProfileService providerProfileService;
    private final CurrentUserService currentUserService;

    @PostMapping("/choose-role")
    @PreAuthorize("isAuthenticated()")
    public SessionResponse chooseRole(@Valid @RequestBody ChooseRoleRequest request) {
        return onboardingService.chooseRole(currentUserService.requireUserId(), request.role());
    }

    @PostMapping("/provider-profile")
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public ProviderProfileResponse providerProfile(@Valid @RequestBody ProviderProfileUpsertRequest request) {
        // This method already:
        // - creates/updates ProviderProfile
        // - creates Business listing (visible to customers)
        // - sets role (safe even if already set)
        return providerProfileService.onboardOrUpdateMyProfile(currentUserService.requireUserId(), request);
    }
}

