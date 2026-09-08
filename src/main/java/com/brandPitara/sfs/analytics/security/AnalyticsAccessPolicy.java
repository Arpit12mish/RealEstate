package com.brandPitara.sfs.analytics.security;

import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authorization boundary for the analytics dashboard read endpoints, mirroring
 * CmsContentAccessPolicy's shape. ADMIN implicitly holds every DashboardPermission
 * (DashboardAuthenticationUserSnapshot#getAuthorities), so no separate role check is
 * needed here - granting ANALYTICS_VIEW to a non-admin staff account is done the same
 * way any other DashboardPermission is assigned (dashboard_user_permissions).
 */
@Component("analyticsAccessPolicy")
public class AnalyticsAccessPolicy {

    public boolean canView(Authentication authentication) {
        return has(authentication, DashboardPermission.ANALYTICS_VIEW);
    }

    public void assertCanView(Authentication authentication) {
        if (!canView(authentication)) {
            throw new AccessDeniedException("Analytics view permission is required");
        }
    }

    private boolean has(Authentication authentication, DashboardPermission permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof DashboardUserDetails user) || !user.isEnabled()) {
            return false;
        }
        return user.getAuthorities().stream()
                .anyMatch(authority -> permission.name().equals(authority.getAuthority()));
    }
}
