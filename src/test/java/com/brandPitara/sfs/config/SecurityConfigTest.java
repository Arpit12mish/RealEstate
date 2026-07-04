package com.brandPitara.sfs.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void publicMobileAuthEndpointsAreExplicitOnly() {
        Set<String> publicEndpoints = Set.of(SecurityConfig.PUBLIC_MOBILE_AUTH_ENDPOINTS);

        assertThat(publicEndpoints).containsExactlyInAnyOrder(
                "/api/auth/request-otp",
                "/api/auth/verify-otp",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/api/auth/logout-all",
                "/api/auth/guest/session"
        );
        assertThat(publicEndpoints).doesNotContain("/api/auth/**", "/api/auth/random");
    }

    @Test
    void publicMobileAuthEndpointsDoNotAccidentallyIncludeUnrelatedPublicRoutes() {
        Set<String> publicEndpoints = Set.of(SecurityConfig.PUBLIC_MOBILE_AUTH_ENDPOINTS);

        // /api/app/screen-content and /api/projects/compare are intentionally public
        // (see SecurityConfig#appFilterChain) but are NOT mobile-auth endpoints and must
        // not be folded into this allowlist.
        assertThat(publicEndpoints).doesNotContain("/api/app/screen-content", "/api/projects/compare");
    }
}
