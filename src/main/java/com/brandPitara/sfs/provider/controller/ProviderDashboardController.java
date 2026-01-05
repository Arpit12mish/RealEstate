package com.brandPitara.sfs.provider.controller;

import com.brandPitara.sfs.provider.dto.ProviderDashboardResponse;
import com.brandPitara.sfs.provider.service.ProviderDashboardService;
import com.brandPitara.sfs.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers/me")
@RequiredArgsConstructor
public class ProviderDashboardController {

    private final ProviderDashboardService dashboardService;
    private final CurrentUserService currentUserService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public ProviderDashboardResponse myDashboard() {
        return dashboardService.getMyDashboard(currentUserService.requireUserId());
    }
}
