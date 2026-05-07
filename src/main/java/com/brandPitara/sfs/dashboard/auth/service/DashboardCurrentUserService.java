package com.brandPitara.sfs.dashboard.auth.service;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;

public interface DashboardCurrentUserService {

    DashboardUserEntity getCurrentUserOrThrow();
}