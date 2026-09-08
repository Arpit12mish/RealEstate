package com.brandPitara.sfs.cms.security;

import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Single authorization boundary for future CMS controllers and services.
 * Ownership is an audit user id, not the public-facing content author id.
 */
@Component("cmsContentAccessPolicy")
public class CmsContentAccessPolicy {

    public boolean canCreate(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_CONTENT_CREATE);
    }

    public void assertCanCreate(Authentication authentication) {
        if (!canCreate(authentication)) {
            throw new AccessDeniedException("CMS content create permission is required");
        }
    }

    public boolean canEdit(Authentication authentication, Long ownerDashboardUserId) {
        DashboardUserDetails user = authenticatedUser(authentication);
        return user != null && (has(user, DashboardPermission.CMS_CONTENT_EDIT_ANY)
                || (ownerDashboardUserId != null
                && ownerDashboardUserId.equals(user.getId())
                && has(user, DashboardPermission.CMS_CONTENT_EDIT_OWN)));
    }

    public boolean canSubmitForReview(Authentication authentication, Long ownerDashboardUserId) {
        return canEdit(authentication, ownerDashboardUserId)
                && has(authentication, DashboardPermission.CMS_CONTENT_SUBMIT_REVIEW);
    }

    public boolean canReview(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_CONTENT_REVIEW);
    }

    public boolean canPublish(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_CONTENT_PUBLISH);
    }

    public boolean canUnpublish(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_CONTENT_UNPUBLISH);
    }

    public boolean canArchive(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_CONTENT_ARCHIVE);
    }

    public boolean canPreview(Authentication authentication, Long ownerDashboardUserId) {
        return has(authentication, DashboardPermission.CMS_CONTENT_PREVIEW)
                && (canEdit(authentication, ownerDashboardUserId)
                || has(authentication, DashboardPermission.CMS_CONTENT_REVIEW)
                || has(authentication, DashboardPermission.CMS_CONTENT_PUBLISH));
    }

    public boolean canView(Authentication authentication, Long ownerDashboardUserId) {
        return canPreview(authentication, ownerDashboardUserId);
    }

    public boolean canViewAll(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_CONTENT_PREVIEW)
                && (has(authentication, DashboardPermission.CMS_CONTENT_EDIT_ANY)
                || has(authentication, DashboardPermission.CMS_CONTENT_REVIEW)
                || has(authentication, DashboardPermission.CMS_CONTENT_PUBLISH));
    }

    public void assertCanView(Authentication authentication, Long ownerDashboardUserId) {
        if (!canView(authentication, ownerDashboardUserId)) {
            throw new AccessDeniedException("CMS content access is required");
        }
    }

    /**
     * Read access to the authors/categories/tags lookup APIs (CmsMetadataController's
     * GET endpoints). CMS_CONTENT_PREVIEW is already granted to every content-staff
     * profile (WRITER, EDITOR, PUBLISHER — see CmsPermissionProfile) and ADMIN
     * implicitly holds every DashboardPermission (DashboardAuthenticationUserSnapshot
     * #getAuthorities), so it's the narrowest existing permission that already means
     * "this is a legitimate CMS content-staff member" without granting any
     * taxonomy-management authority — mutation stays on hasRole('ADMIN') directly on
     * the controller and is untouched by this check.
     */
    public boolean canReadTaxonomy(Authentication authentication) {
        return has(authentication, DashboardPermission.CMS_CONTENT_PREVIEW);
    }

    public void assertCanReadTaxonomy(Authentication authentication) {
        if (!canReadTaxonomy(authentication)) {
            throw new AccessDeniedException("CMS content preview permission is required");
        }
    }

    public void assertCanEdit(Authentication authentication, Long ownerDashboardUserId) {
        if (!canEdit(authentication, ownerDashboardUserId)) {
            throw new AccessDeniedException("CMS content edit permission is required");
        }
    }

    public void assertCanSubmitForReview(Authentication authentication, Long ownerDashboardUserId) {
        if (!canSubmitForReview(authentication, ownerDashboardUserId)) {
            throw new AccessDeniedException("CMS content submit-for-review permission is required");
        }
    }

    public void assertCanReview(Authentication authentication) {
        if (!canReview(authentication)) {
            throw new AccessDeniedException("CMS content review permission is required");
        }
    }

    public void assertCanPublish(Authentication authentication) {
        if (!canPublish(authentication)) {
            throw new AccessDeniedException("CMS content publish permission is required");
        }
    }

    public void assertCanUnpublish(Authentication authentication) {
        if (!canUnpublish(authentication)) {
            throw new AccessDeniedException("CMS content unpublish permission is required");
        }
    }

    public void assertCanArchive(Authentication authentication) {
        if (!canArchive(authentication)) {
            throw new AccessDeniedException("CMS content archive permission is required");
        }
    }

    private boolean has(Authentication authentication, DashboardPermission permission) {
        DashboardUserDetails user = authenticatedUser(authentication);
        return user != null && has(user, permission);
    }

    private boolean has(DashboardUserDetails user, DashboardPermission permission) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> permission.name().equals(authority.getAuthority()));
    }

    private DashboardUserDetails authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof DashboardUserDetails details
                && details.isEnabled() ? details : null;
    }
}
