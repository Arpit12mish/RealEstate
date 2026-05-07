package com.brandPitara.sfs.dashboard.overview.controller;

import com.brandPitara.sfs.dashboard.overview.dto.DashboardOverviewResponse;
import com.brandPitara.sfs.dashboard.overview.service.DashboardOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/overview")
@RequiredArgsConstructor
public class DashboardOverviewController {

    private final DashboardOverviewService dashboardOverviewService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardOverviewResponse overview() {
        return dashboardOverviewService.getOverview();
    }
}
