package com.brandPitara.sfs.dashboard.auth.dto;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardUserResponse {

    private Long id;
    private String name;
    private String email;
    private DashboardRole role;
    private Boolean active;
    private Set<DashboardPermission> permissions;
    private Set<CmsPermissionProfile> permissionProfiles;

    public static DashboardUserResponse from(DashboardUserEntity user) {
        if (user == null) {
            return null;
        }

        Set<DashboardPermission> permissions = user.getRole() == DashboardRole.ADMIN
                ? Set.of(DashboardPermission.values())
                : Set.copyOf(user.getPermissions());
        return DashboardUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .permissions(permissions)
                .permissionProfiles(CmsPermissionProfile.matchingProfiles(permissions))
                .build();
    }
}
