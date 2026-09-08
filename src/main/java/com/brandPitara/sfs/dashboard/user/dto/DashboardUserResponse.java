package com.brandPitara.sfs.dashboard.user.dto;

import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;

import java.time.OffsetDateTime;
import java.util.Set;

public record DashboardUserResponse(
        Long id,
        String email,
        String displayName,
        DashboardRole role,
        boolean active,
        Set<DashboardPermission> permissions,
        Set<CmsPermissionProfile> permissionProfiles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DashboardUserResponse from(DashboardUserEntity user) {
        Set<DashboardPermission> permissions = Set.copyOf(user.getPermissions());
        return new DashboardUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isActiveUser(),
                permissions,
                CmsPermissionProfile.matchingProfiles(permissions),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
