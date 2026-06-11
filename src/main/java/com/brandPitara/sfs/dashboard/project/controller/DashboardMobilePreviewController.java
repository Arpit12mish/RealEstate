package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.project.dto.DashboardMobilePreviewResponse;
import com.brandPitara.sfs.dashboard.project.service.DashboardMobilePreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/projects/{projectId}/mobile-preview")
@RequiredArgsConstructor
public class DashboardMobilePreviewController {

    private final DashboardMobilePreviewService dashboardMobilePreviewService;

    /**
     * Combined mobile preview: card + detail + meter + diagnostics meta.
     * Works for draft, unpublished, and unapproved projects.
     * Save the dashboard form before refreshing this preview.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardMobilePreviewResponse getPreview(@PathVariable Long projectId) {
        return dashboardMobilePreviewService.getPreview(projectId);
    }
}
