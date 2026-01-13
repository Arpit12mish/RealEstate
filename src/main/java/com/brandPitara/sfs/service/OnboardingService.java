package com.brandPitara.sfs.service;


import com.brandPitara.sfs.enums.OnboardingRole;
import com.brandPitara.sfs.dto.onboarding.SessionResponse;

public interface OnboardingService {
    SessionResponse chooseRole(Long userId, OnboardingRole role);

    SessionResponse getSession(Long userId);
}

