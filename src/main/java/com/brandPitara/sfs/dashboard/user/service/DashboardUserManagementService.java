package com.brandPitara.sfs.dashboard.user.service;

import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserCreateRequest;
import com.brandPitara.sfs.dashboard.user.dto.DashboardUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface DashboardUserManagementService {
    DashboardUserResponse create(DashboardUserCreateRequest request);

    Page<DashboardUserResponse> list(
            DashboardRole role,
            Boolean active,
            String search,
            Pageable pageable
    );

    DashboardUserResponse get(Long userId);

    DashboardUserResponse updateCmsPermissions(Long userId, Set<CmsPermissionProfile> profiles);

    DashboardUserResponse setActive(Long userId, boolean active);

    void resetPassword(Long userId, String newPassword);
}
