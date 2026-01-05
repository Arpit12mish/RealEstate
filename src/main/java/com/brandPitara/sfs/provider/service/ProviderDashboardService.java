package com.brandPitara.sfs.provider.service;

import com.brandPitara.sfs.provider.dto.ProviderDashboardResponse;

public interface ProviderDashboardService {
    ProviderDashboardResponse getMyDashboard(Long currentUserId);
}
