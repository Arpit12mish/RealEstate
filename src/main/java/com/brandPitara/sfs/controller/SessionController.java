package com.brandPitara.sfs.controller;


import com.brandPitara.sfs.dto.onboarding.SessionResponse;
import com.brandPitara.sfs.service.OnboardingService;
import com.brandPitara.sfs.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final OnboardingService onboardingService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public SessionResponse me() {
        return onboardingService.getSession(currentUserService.requireUserId());
    }
}

