package com.brandPitara.sfs.cms.media.security;

import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("cmsMediaAccessPolicy")
public class CmsMediaAccessPolicy {

    public boolean canUpload(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_MEDIA_UPLOAD);
    }

    public boolean canRead(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_MEDIA_UPLOAD)
                || has(authentication, DashboardPermission.CMS_CONTENT_PREVIEW);
    }

    public boolean canFinalize(Authentication authentication, Long creatorId) {
        DashboardUserDetails user = user(authentication);
        return user != null && canUpload(authentication)
                && (user.getId().equals(creatorId) || "ADMIN".equals(user.getRoleName()));
    }

    public Long userId(Authentication authentication) {
        DashboardUserDetails user = user(authentication);
        return user == null ? null : user.getId();
    }

    private boolean has(Authentication authentication, DashboardPermission permission) {
        DashboardUserDetails user = user(authentication);
        return user != null && user.getAuthorities().stream()
                .anyMatch(authority -> permission.name().equals(authority.getAuthority()));
    }

    private DashboardUserDetails user(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof DashboardUserDetails details)
                || !details.isEnabled()) {
            return null;
        }
        return details;
    }
}
