package com.brandPitara.sfs.dashboard.auth.service;

import com.brandPitara.sfs.dashboard.auth.dto.*;
import jakarta.servlet.http.HttpServletRequest;

public interface DashboardAuthService {

    DashboardAuthResponse login(DashboardLoginRequest request, HttpServletRequest httpRequest);

    DashboardAuthResponse refresh(DashboardRefreshRequest request);

    void logout(DashboardLogoutRequest request);

    DashboardUserResponse me();
}