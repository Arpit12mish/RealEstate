package com.brandPitara.sfs.company.security;

import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GAP-034 hardening, security/rate-limit slice. Uses the REAL
 * RateLimitPolicyResolver (not a mock of the policy result), per Phase
 * 7A-G's explicit instruction. RateLimitPolicyResolverTest already covers
 * these exact routes (see publicCompanyGetRoutesMapToPublicCompanyRead /
 * publicArchitectDesignerGetRouteMapsToPublicArchitectDesignerRead) - this
 * file re-asserts the subset this phase's contract directly depends on so a
 * future unrelated change to that shared file cannot silently break Company
 * coverage without this phase's own suite noticing. Not a redesign or
 * modification of RateLimitPolicyResolver itself.
 */
class CompanyRateLimitRegressionTest {

    private final RateLimitPolicyResolver resolver = new RateLimitPolicyResolver();

    private HttpServletRequest request(String method, String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("");
        return request;
    }

    @Test
    void companyListingResolvesToPublicCompanyRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/companies")))
            .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
    }

    @Test
    void companyDetailResolvesToPublicCompanyRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/companies/701")))
            .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
    }

    @Test
    void companyDetailWithANonNumericPathSegmentStillResolvesToPublicCompanyRead() {
        // The rate-limit resolver matches on path shape alone (AntPathMatcher),
        // before Spring's @PathVariable Long conversion runs - so this still
        // resolves correctly even for the GAP-032 non-numeric-ID case.
        assertThat(resolver.resolve(request("GET", "/api/public/companies/not-a-number")))
            .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
    }

    @Test
    void architectDesignerRoutesAreUnaffectedByCompanyRouteWiring() {
        assertThat(resolver.resolve(request("GET", "/api/public/architect-designers/42")))
            .contains(RateLimitPolicy.PUBLIC_ARCHITECT_DESIGNER_READ);
    }

    @Test
    void companyProjectRoutesAreUnaffectedByCompanyRouteWiring() {
        assertThat(resolver.resolve(request("GET", "/api/public/company-projects/7")))
            .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
    }

    @Test
    void writeMethodsAgainstCompanyRoutesDoNotResolveToAnyReadPolicy() {
        assertThat(resolver.resolve(request("POST", "/api/public/companies"))).isEmpty();
        assertThat(resolver.resolve(request("PUT", "/api/public/companies/701"))).isEmpty();
        assertThat(resolver.resolve(request("DELETE", "/api/public/companies/701"))).isEmpty();
    }

    @Test
    void dashboardAndAdminCompanyPathsNeverResolveToThePublicPolicy() {
        // No dashboard Company controller is exposed under /api/public/**; this
        // locks in that even a hypothetical /api/dashboard/companies path would
        // not accidentally match the public-prefix rule.
        assertThat(resolver.resolve(request("GET", "/api/dashboard/companies"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/admin/companies"))).isEmpty();
    }
}
