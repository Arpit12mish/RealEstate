package com.brandPitara.sfs.dashboard.user.dto;

import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record DashboardUserCmsPermissionsRequest(
        @NotNull Set<CmsPermissionProfile> permissionProfiles
) {
    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignoredValue) {
        throw new IllegalArgumentException("Unknown CMS-permissions field: " + field);
    }
}
