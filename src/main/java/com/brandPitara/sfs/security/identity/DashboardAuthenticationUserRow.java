package com.brandPitara.sfs.security.identity;

import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;

/** One password-free row of a dashboard authentication snapshot. */
public record DashboardAuthenticationUserRow(
        Long userId,
        String email,
        String name,
        DashboardRole role,
        boolean active,
        DashboardPermission permission
) {
}
