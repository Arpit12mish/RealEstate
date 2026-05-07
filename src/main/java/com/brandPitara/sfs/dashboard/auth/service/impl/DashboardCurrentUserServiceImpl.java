package com.brandPitara.sfs.dashboard.auth.service.impl;

import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DashboardCurrentUserServiceImpl implements DashboardCurrentUserService {

    @Override
    public DashboardUserEntity getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof DashboardUserDetails details)) {
            throw new BadCredentialsException("Dashboard authentication required");
        }

        DashboardUserEntity user = details.getUser();

        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            throw new BadCredentialsException("Dashboard user is disabled or missing");
        }

        return user;
    }
}