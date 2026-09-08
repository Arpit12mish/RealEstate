package com.brandPitara.sfs.config;

import com.brandPitara.sfs.dashboard.auth.security.DashboardJwtAuthenticationFilter;
import com.brandPitara.sfs.ratelimit.config.RateLimitFilterRegistrationConfig;
import com.brandPitara.sfs.ratelimit.filter.PreAuthenticationAbuseFilter;
import com.brandPitara.sfs.ratelimit.filter.RateLimitingFilter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityFilterRegistrationConfigTest {

    @Test
    void securityChainFiltersAreNotAutoRegisteredByServletContainer() {
        SecurityFilterRegistrationConfig security = new SecurityFilterRegistrationConfig();
        RateLimitFilterRegistrationConfig rateLimit = new RateLimitFilterRegistrationConfig();

        assertThat(security.jwtRequestFilterRegistration(mock(JwtRequestFilter.class)).isEnabled()).isFalse();
        assertThat(security.dashboardJwtFilterRegistration(mock(DashboardJwtAuthenticationFilter.class)).isEnabled()).isFalse();
        assertThat(rateLimit.rateLimitingFilterRegistration(mock(RateLimitingFilter.class)).isEnabled()).isFalse();
        assertThat(rateLimit.preAuthenticationAbuseFilterRegistration(mock(PreAuthenticationAbuseFilter.class)).isEnabled()).isFalse();
    }
}
