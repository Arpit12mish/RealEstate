package com.brandPitara.sfs.dashboard.project.service;

import com.brandPitara.sfs.dashboard.project.dto.DashboardMobilePreviewResponse;

public interface DashboardMobilePreviewService {
    DashboardMobilePreviewResponse getPreview(Long projectId);
}
