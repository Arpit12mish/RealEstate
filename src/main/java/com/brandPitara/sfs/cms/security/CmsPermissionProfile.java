package com.brandPitara.sfs.cms.security;

import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.brandPitara.sfs.dashboard.common.enums.DashboardPermission.*;

/**
 * Administrative assignment templates, not Spring Security roles. A staff
 * account may receive more than one profile or an explicitly tailored set.
 */
public enum CmsPermissionProfile {
    WRITER(Set.of(
            CMS_CONTENT_CREATE,
            CMS_CONTENT_EDIT_OWN,
            CMS_CONTENT_SUBMIT_REVIEW,
            CMS_CONTENT_PREVIEW,
            CMS_MEDIA_UPLOAD
    )),
    EDITOR(Set.of(
            CMS_CONTENT_CREATE,
            CMS_CONTENT_EDIT_OWN,
            CMS_CONTENT_EDIT_ANY,
            CMS_CONTENT_SUBMIT_REVIEW,
            CMS_CONTENT_REVIEW,
            CMS_CONTENT_PREVIEW,
            CMS_MEDIA_UPLOAD
    )),
    PUBLISHER(Set.of(
            CMS_CONTENT_PREVIEW,
            CMS_CONTENT_PUBLISH,
            CMS_CONTENT_UNPUBLISH,
            CMS_CONTENT_ARCHIVE
    ));

    private final Set<DashboardPermission> permissions;

    CmsPermissionProfile(Set<DashboardPermission> permissions) {
        this.permissions = permissions;
    }

    public Set<DashboardPermission> permissions() {
        return permissions;
    }

    public static Set<DashboardPermission> resolve(Set<CmsPermissionProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return Set.of();
        }
        EnumSet<DashboardPermission> resolved = EnumSet.noneOf(DashboardPermission.class);
        profiles.forEach(profile -> resolved.addAll(profile.permissions));
        return Collections.unmodifiableSet(resolved);
    }

    public static Set<CmsPermissionProfile> matchingProfiles(Set<DashboardPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        EnumSet<CmsPermissionProfile> matches = EnumSet.noneOf(CmsPermissionProfile.class);
        Arrays.stream(values())
                .filter(profile -> permissions.containsAll(profile.permissions))
                .filter(profile -> Arrays.stream(values()).noneMatch(other ->
                        other != profile
                                && permissions.containsAll(other.permissions)
                                && other.permissions.containsAll(profile.permissions)
                ))
                .forEach(matches::add);
        return Collections.unmodifiableSet(matches);
    }
}
