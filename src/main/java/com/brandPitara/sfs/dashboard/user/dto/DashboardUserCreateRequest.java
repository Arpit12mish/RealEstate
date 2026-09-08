package com.brandPitara.sfs.dashboard.user.dto;

import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record DashboardUserCreateRequest(
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 150) String displayName,
        @NotNull DashboardRole role,
        @NotNull Set<CmsPermissionProfile> permissionProfiles,
        @NotBlank @Size(min = 12, max = 128) String initialPassword
) {
    @JsonAnySetter
    public void rejectUnknownField(String field, Object ignoredValue) {
        throw new IllegalArgumentException("Unknown dashboard-user field: " + field);
    }
}
