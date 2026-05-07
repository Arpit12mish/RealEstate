package com.brandPitara.sfs.dashboard.audit.service;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import jakarta.servlet.http.HttpServletRequest;

public interface DashboardLoginAuditService {

    void recordSuccess(DashboardUserEntity user, HttpServletRequest request);

    void recordFailure(String email, String reason, HttpServletRequest request);
}