package com.brandPitara.sfs.service;


import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.dto.onboarding.SessionResponse;

public interface OnboardingService {
    SessionResponse chooseRole(Long userId, Role role);
    SessionResponse getSession(Long userId);
}

