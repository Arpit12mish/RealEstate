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
        assertThat(resolver.resolve(request("GET", "/api/unknown-resource/1"))).isEmpty();
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

    // ── Phase 1.5: remaining public mobile/public read APIs ─────────────────────

    @Test
    void getCitiesMapsToPublicCityRead() {
        assertThat(resolver.resolve(request("GET", "/api/cities")))
                .contains(RateLimitPolicy.PUBLIC_CITY_READ);
    }

    @Test
    void getCitiesWithQueryParamsStillMapsToPublicCityRead() {
        // Query strings are never part of HttpServletRequest#getRequestURI(), so a
        // request for /api/cities?homepageFeatured=true resolves on the same path.
        assertThat(resolver.resolve(request("GET", "/api/cities")))
                .contains(RateLimitPolicy.PUBLIC_CITY_READ);
    }

    @Test
    void postCitiesDoesNotMapToPublicCityRead() {
        assertThat(resolver.resolve(request("POST", "/api/cities"))).isEmpty();
    }

    @Test
    void getBuilderDetailMapsToPublicBuilderRead() {
        assertThat(resolver.resolve(request("GET", "/api/builders/123")))
                .contains(RateLimitPolicy.PUBLIC_BUILDER_READ);
    }

    @Test
    void postBuildersDoesNotMapToPublicBuilderRead() {
        assertThat(resolver.resolve(request("POST", "/api/builders"))).isEmpty();
    }

    @Test
    void getBusinessDetailMapsToPublicBusinessRead() {
        assertThat(resolver.resolve(request("GET", "/api/businesses/123")))
                .contains(RateLimitPolicy.PUBLIC_BUSINESS_READ);
    }

    @Test
    void postBusinessesDoesNotMapToPublicBusinessRead() {
        assertThat(resolver.resolve(request("POST", "/api/businesses"))).isEmpty();
    }

    @Test
    void getProviderDetailMapsToPublicProviderRead() {
        assertThat(resolver.resolve(request("GET", "/api/providers/123")))
                .contains(RateLimitPolicy.PUBLIC_PROVIDER_READ);
    }

    @Test
    void postProvidersDoesNotMapToPublicProviderRead() {
        assertThat(resolver.resolve(request("POST", "/api/providers"))).isEmpty();
    }

    @Test
    void getAppContentMapsToPublicAppContentRead() {
        assertThat(resolver.resolve(request("GET", "/api/app-content/pages/about-us")))
                .contains(RateLimitPolicy.PUBLIC_APP_CONTENT_READ);
    }

    @Test
    void getAppScreenContentMapsToPublicAppContentRead() {
        assertThat(resolver.resolve(request("GET", "/api/app/screen-content")))
                .contains(RateLimitPolicy.PUBLIC_APP_CONTENT_READ);
    }

    @Test
    void postAppScreenContentDoesNotMapToPublicAppContentRead() {
        assertThat(resolver.resolve(request("POST", "/api/app/screen-content"))).isEmpty();
    }

    @Test
    void getStampDutyRoutesMapToPublicCalculatorRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/stamp-duty/cities")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/stamp-duty/buyer-types")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
    }

    @Test
    void getInteriorCostRoutesMapToPublicCalculatorRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/interior-cost/cities")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/interior-cost/bhk-types")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
    }

    @Test
    void getCircleRateAndCalculatorCardRoutesMapToPublicCalculatorRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/circle-rates/cities")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/calculators/cards")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
    }

    @Test
    void writeVerbsNeverMatchAnyOfThePhase1_5ReadPolicies() {
        assertThat(resolver.resolve(request("PUT", "/api/cities/1"))).isEmpty();
        assertThat(resolver.resolve(request("DELETE", "/api/builders/1"))).isEmpty();
        assertThat(resolver.resolve(request("PATCH", "/api/businesses/1"))).isEmpty();
        assertThat(resolver.resolve(request("PUT", "/api/providers/1"))).isEmpty();
        assertThat(resolver.resolve(request("POST", "/api/app-content/pages/about-us"))).isEmpty();
    }

    @Test
    void unsupportedPublicRouteResolvesToEmptyUnlessIntentionallyConfigured() {
        // Genuinely public (per SecurityConfig's broad /api/public/** GET permitAll)
        // but not yet mapped to a Phase 1/1.5 policy - intentionally out of scope
        // for this PR (see deliverables: companies, architect-designers, feed, etc).
        assertThat(resolver.resolve(request("GET", "/api/public/companies"))).isEmpty();
    }
}
