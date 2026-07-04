package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitPolicyResolverTest {

    private final RateLimitPolicyResolver resolver = new RateLimitPolicyResolver();

    private HttpServletRequest request(String method, String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("");
        return request;
    }

    @Test
    void mapsEachDocumentedRouteToItsPolicy() {
        assertThat(resolver.resolve(request("POST", "/api/auth/request-otp")))
                .contains(RateLimitPolicy.MOBILE_OTP_REQUEST);
        assertThat(resolver.resolve(request("POST", "/api/auth/verify-otp")))
                .contains(RateLimitPolicy.MOBILE_OTP_VERIFY);
        assertThat(resolver.resolve(request("POST", "/api/auth/refresh")))
                .contains(RateLimitPolicy.MOBILE_TOKEN_REFRESH);
        assertThat(resolver.resolve(request("POST", "/api/auth/logout")))
                .contains(RateLimitPolicy.MOBILE_LOGOUT);
        assertThat(resolver.resolve(request("POST", "/api/auth/logout-all")))
                .contains(RateLimitPolicy.MOBILE_LOGOUT_ALL);
        assertThat(resolver.resolve(request("POST", "/api/auth/guest/session")))
                .contains(RateLimitPolicy.MOBILE_GUEST_SESSION);
        assertThat(resolver.resolve(request("GET", "/api/home")))
                .contains(RateLimitPolicy.PUBLIC_HOME_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/home")))
                .contains(RateLimitPolicy.PUBLIC_HOME_READ);
        assertThat(resolver.resolve(request("GET", "/api/projects/42")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_READ);
        assertThat(resolver.resolve(request("POST", "/api/projects/compare")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_COMPARE);
        assertThat(resolver.resolve(request("POST", "/api/location/resolve")))
                .contains(RateLimitPolicy.PUBLIC_LOCATION_RESOLVE);
        assertThat(resolver.resolve(request("GET", "/api/public/search")))
                .contains(RateLimitPolicy.PUBLIC_SEARCH);
        assertThat(resolver.resolve(request("GET", "/api/search/businesses")))
                .contains(RateLimitPolicy.PUBLIC_SEARCH);
    }

    @Test
    void projectsCompareDoesNotClashWithGenericProjectReadPolicy() {
        // Same path prefix, different HTTP method - must resolve to distinct policies.
        assertThat(resolver.resolve(request("POST", "/api/projects/compare")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_COMPARE);
        assertThat(resolver.resolve(request("GET", "/api/projects/compare")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_READ);
    }

    @Test
    void unmappedRouteResolvesToEmpty() {
        assertThat(resolver.resolve(request("GET", "/api/builders/1"))).isEmpty();
        assertThat(resolver.resolve(request("DELETE", "/api/auth/request-otp"))).isEmpty();
    }

    // ── Check 4: method-aware matching, no overmatch on write verbs ─────────────

    @Test
    void getProjectDetailMapsToPublicProjectRead() {
        assertThat(resolver.resolve(request("GET", "/api/projects/123")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_READ);
    }

    @Test
    void postProjectsCompareMapsToPublicProjectCompare() {
        assertThat(resolver.resolve(request("POST", "/api/projects/compare")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_COMPARE);
    }

    @Test
    void postSubResourceUnderProjectsDoesNotMatchPublicProjectRead() {
        assertThat(resolver.resolve(request("POST", "/api/projects/123/reviews"))).isEmpty();
    }

    @Test
    void putProjectDoesNotMatchPublicProjectRead() {
        assertThat(resolver.resolve(request("PUT", "/api/projects/123"))).isEmpty();
    }

    @Test
    void deleteProjectDoesNotMatchPublicProjectRead() {
        assertThat(resolver.resolve(request("DELETE", "/api/projects/123"))).isEmpty();
    }

    @Test
    void patchProjectDoesNotMatchPublicProjectRead() {
        assertThat(resolver.resolve(request("PATCH", "/api/projects/123"))).isEmpty();
    }

    @Test
    void homeAndPublicHomeBothMapToPublicHomeRead() {
        assertThat(resolver.resolve(request("GET", "/api/home")))
                .contains(RateLimitPolicy.PUBLIC_HOME_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/home")))
                .contains(RateLimitPolicy.PUBLIC_HOME_READ);
    }

    @Test
    void postToSearchHasNoPhase1Policy() {
        assertThat(resolver.resolve(request("POST", "/api/search/whatever"))).isEmpty();
        assertThat(resolver.resolve(request("POST", "/api/public/search"))).isEmpty();
    }
}
