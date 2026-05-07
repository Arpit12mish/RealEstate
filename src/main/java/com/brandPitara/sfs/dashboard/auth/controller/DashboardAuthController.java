package com.brandPitara.sfs.dashboard.auth.controller;

import com.brandPitara.sfs.dashboard.auth.dto.*;
import com.brandPitara.sfs.dashboard.auth.service.DashboardAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/auth")
@RequiredArgsConstructor
public class DashboardAuthController {

    private final DashboardAuthService dashboardAuthService;

    @PostMapping("/login")
    public DashboardAuthResponse login(
            @Valid @RequestBody DashboardLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return dashboardAuthService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    public DashboardAuthResponse refresh(@Valid @RequestBody DashboardRefreshRequest request) {
        return dashboardAuthService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody DashboardLogoutRequest request) {
        dashboardAuthService.logout(request);
    }

    @GetMapping("/me")
    public DashboardUserResponse me() {
        return dashboardAuthService.me();
    }
}