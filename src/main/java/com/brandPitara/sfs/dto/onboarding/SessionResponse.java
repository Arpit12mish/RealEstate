package com.brandPitara.sfs.dto.onboarding;

import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;

public record SessionResponse(
        Long userId,
        String phoneNumber,
        Role role,
        OnboardingStatus onboardingStatus,
        String nextAction,
        Long providerId,
        Long businessId
) {}

