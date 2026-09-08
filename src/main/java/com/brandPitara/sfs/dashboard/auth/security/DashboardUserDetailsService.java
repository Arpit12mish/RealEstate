package com.brandPitara.sfs.dashboard.auth.security;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationIdentityCache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DashboardUserDetailsService implements UserDetailsService {

    private final DashboardUserRepository dashboardUserRepository;
    private final DashboardAuthenticationIdentityCache identityCache;

    public DashboardUserDetails loadById(Long userId) {
        return new DashboardUserDetails(identityCache.get(userId));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (!StringUtils.hasText(email)) {
            throw new UsernameNotFoundException("Dashboard user email is required");
        }

        DashboardUserEntity user = dashboardUserRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Dashboard user not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException("Dashboard user is disabled");
        }

        return new DashboardUserDetails(user);
    }
}
