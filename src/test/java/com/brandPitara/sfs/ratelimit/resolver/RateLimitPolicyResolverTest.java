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
        // As of Phase 2, POST .../reviews is intentionally mapped to
        // MOBILE_REVIEW_WRITE (a distinct policy) - the original guarantee this
        // test protects (POST never falls through to the GET-only
        // PUBLIC_PROJECT_READ policy) still holds.
        // An Optional holds at most one value, so proving it's MOBILE_REVIEW_WRITE
        // already proves it is not PUBLIC_PROJECT_READ.
        assertThat(resolver.resolve(request("POST", "/api/projects/123/reviews")))
                .contains(RateLimitPolicy.MOBILE_REVIEW_WRITE);
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
    void getBuilderSlugDetailMapsToPublicBuilderRead() {
        // Phase 6B-G: the new canonical slug-based detail lookup falls under the same
        // existing GET /api/builders/** -> PUBLIC_BUILDER_READ wildcard rule already
        // covering the numeric-id route above - no new Route entry was needed.
        assertThat(resolver.resolve(request("GET", "/api/builders/slug/meridian-constructions")))
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
        // As of Phase 4, brands/distributors/categories/trending-city/content-version/
        // session-me are all mapped (see Phase 4 tests below) - mobile/public coverage
        // is now complete, so this guard uses a genuinely nonexistent path instead.
        assertThat(resolver.resolve(request("GET", "/api/public/does-not-exist"))).isEmpty();
    }

    // ── Phase 2: mobile action APIs ──────────────────────────────────────────────

    @Test
    void publicCalculatorPostRoutesMapToPublicCalculatorWrite() {
        assertThat(resolver.resolve(request("POST", "/api/public/stamp-duty/calculate")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/public/interior-cost/compare")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/public/interior-cost/compare-custom")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/public/circle-rates/calculate")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE);
    }

    @Test
    void calculatorGetRoutesStillMapToPublicCalculatorRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/stamp-duty/cities")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/interior-cost/cities")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
    }

    @Test
    void unsupportedCalculatorMethodsDoNotAccidentallyMap() {
        assertThat(resolver.resolve(request("PUT", "/api/public/stamp-duty/calculate"))).isEmpty();
        assertThat(resolver.resolve(request("DELETE", "/api/public/interior-cost/compare"))).isEmpty();
        // GET on the same path legitimately falls under the existing broad
        // GET /api/public/stamp-duty/** -> PUBLIC_CALCULATOR_READ rule, not a new
        // policy and not PUBLIC_CALCULATOR_WRITE (which is POST-only) - this is
        // correct pre-existing behavior, not an accidental match.
        assertThat(resolver.resolve(request("GET", "/api/public/stamp-duty/calculate")))
                .contains(RateLimitPolicy.PUBLIC_CALCULATOR_READ);
    }

    @Test
    void profileGetMapsToMobileProfileRead() {
        assertThat(resolver.resolve(request("GET", "/api/profile")))
                .contains(RateLimitPolicy.MOBILE_PROFILE_READ);
    }

    @Test
    void profilePatchPutPostRouteMapsToMobileProfileWrite() {
        assertThat(resolver.resolve(request("PUT", "/api/profile")))
                .contains(RateLimitPolicy.MOBILE_PROFILE_WRITE);
        assertThat(resolver.resolve(request("PATCH", "/api/profile")))
                .contains(RateLimitPolicy.MOBILE_PROFILE_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/profile")))
                .contains(RateLimitPolicy.MOBILE_PROFILE_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/profile/photo/confirm")))
                .contains(RateLimitPolicy.MOBILE_PROFILE_WRITE);
        assertThat(resolver.resolve(request("DELETE", "/api/profile/account")))
                .contains(RateLimitPolicy.MOBILE_PROFILE_WRITE);
    }

    @Test
    void profilePhotoPresignMapsToMobileMediaOrUploadActionNotProfileWrite() {
        assertThat(resolver.resolve(request("POST", "/api/profile/photo/presign")))
                .contains(RateLimitPolicy.MOBILE_MEDIA_OR_UPLOAD_ACTION);
    }

    @Test
    void submittedReviewsGetTakesPriorityOverProfileReadPolicy() {
        // Specific path must win: /api/profile/submitted-reviews is a distinct
        // controller/purpose from the generic /api/profile "my profile" read.
        assertThat(resolver.resolve(request("GET", "/api/profile/submitted-reviews")))
                .contains(RateLimitPolicy.MOBILE_REVIEW_READ);
        assertThat(resolver.resolve(request("GET", "/api/profile")))
                .contains(RateLimitPolicy.MOBILE_PROFILE_READ);
    }

    @Test
    void favoritesGetRouteMapsToMobileFavoriteRead() {
        assertThat(resolver.resolve(request("GET", "/api/project-favorites")))
                .contains(RateLimitPolicy.MOBILE_FAVORITE_READ);
        assertThat(resolver.resolve(request("GET", "/api/project-favorites/42/exists")))
                .contains(RateLimitPolicy.MOBILE_FAVORITE_READ);
    }

    @Test
    void favoritesPostDeleteRouteMapsToMobileFavoriteWrite() {
        assertThat(resolver.resolve(request("POST", "/api/project-favorites/42/toggle")))
                .contains(RateLimitPolicy.MOBILE_FAVORITE_WRITE);
        assertThat(resolver.resolve(request("DELETE", "/api/project-favorites/42")))
                .contains(RateLimitPolicy.MOBILE_FAVORITE_WRITE);
    }

    @Test
    void reviewPostRouteMapsToMobileReviewWrite() {
        assertThat(resolver.resolve(request("POST", "/api/projects/42/reviews")))
                .contains(RateLimitPolicy.MOBILE_REVIEW_WRITE);
    }

    @Test
    void publicProjectReviewReadIsNotBlockedByMobileReviewPolicies() {
        // Public review reads already fall under the existing broad
        // GET /api/projects/** -> PUBLIC_PROJECT_READ rule - must not be
        // reclassified under a Phase 2 mobile policy.
        assertThat(resolver.resolve(request("GET", "/api/projects/42/reviews")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_READ);
        assertThat(resolver.resolve(request("GET", "/api/projects/42/public-review-signal")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_READ);
    }

    @Test
    void providerMeGetTakesPriorityOverPublicProviderReadPolicy() {
        assertThat(resolver.resolve(request("GET", "/api/providers/me/profile")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_READ);
        assertThat(resolver.resolve(request("GET", "/api/providers/me/projects")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_READ);
        assertThat(resolver.resolve(request("GET", "/api/providers/me/media")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_READ);
        assertThat(resolver.resolve(request("GET", "/api/providers/me/dashboard")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_READ);

        // Regression: a genuinely public, non-"me" provider route must be unaffected.
        assertThat(resolver.resolve(request("GET", "/api/providers/123")))
                .contains(RateLimitPolicy.PUBLIC_PROVIDER_READ);
    }

    @Test
    void providerMeWriteRoutesMapToMobileProviderAccountWrite() {
        assertThat(resolver.resolve(request("POST", "/api/providers/me/profile")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/providers/me/projects")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE);
        assertThat(resolver.resolve(request("DELETE", "/api/providers/me/projects/7")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/providers/me/media/gallery")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE);
    }

    @Test
    void providerMediaPresignTakesPriorityOverProviderAccountWritePolicy() {
        assertThat(resolver.resolve(request("POST", "/api/providers/me/media/presign")))
                .contains(RateLimitPolicy.MOBILE_MEDIA_OR_UPLOAD_ACTION);
    }

    @Test
    void dashboardPathsNeverMapToMobilePolicies() {
        assertThat(resolver.resolve(request("GET", "/api/dashboard/profile"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/dashboard/providers/me/profile"))).isEmpty();
        assertThat(resolver.resolve(request("POST", "/api/dashboard/project-favorites/1/toggle"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/admin/stamp-duty"))).isEmpty();
    }

    @Test
    void broadRouteMatchingDoesNotAccidentallyRateLimitUnrelatedWriteRoutes() {
        // A provider write route outside the /me/ prefix must not be swept in.
        assertThat(resolver.resolve(request("POST", "/api/providers/123/something"))).isEmpty();
        // Unrelated write routes elsewhere remain unaffected by Phase 2 additions.
        assertThat(resolver.resolve(request("PUT", "/api/businesses/1"))).isEmpty();
        assertThat(resolver.resolve(request("POST", "/api/builders"))).isEmpty();
    }

    // ── Phase 3: remaining mobile/public API rate limiting ──────────────────────

    @Test
    void customerServiceRequestPostRoutesMapToMobileServiceRequestWrite() {
        assertThat(resolver.resolve(request("POST", "/api/customer/requests")))
                .contains(RateLimitPolicy.MOBILE_SERVICE_REQUEST_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/customer/requests/42/close")))
                .contains(RateLimitPolicy.MOBILE_SERVICE_REQUEST_WRITE);
    }

    @Test
    void customerServiceRequestGetRoutesAreNotMappedThisPhase() {
        // Deliberately deferred - not named in this phase's scope (see coverage matrix).
        assertThat(resolver.resolve(request("GET", "/api/customer/requests"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/customer/requests/42/interests"))).isEmpty();
    }

    @Test
    void providerInterestPostRouteMapsToMobileProviderInterestWrite() {
        assertThat(resolver.resolve(request("POST", "/api/providers/me/requests/42/interest")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_INTEREST_WRITE);
    }

    @Test
    void providerInterestTakesPriorityOverGenericProviderAccountWritePolicy() {
        // Specific must win: without correct ordering this would fall through to
        // the broad MOBILE_PROVIDER_ACCOUNT_WRITE rule for /api/providers/me/**.
        assertThat(resolver.resolve(request("POST", "/api/providers/me/requests/42/interest")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_INTEREST_WRITE);
        // Regression: other provider "me" writes are unaffected.
        assertThat(resolver.resolve(request("POST", "/api/providers/me/profile")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE);
    }

    @Test
    void providerServiceRequestListGetIsCoveredByExistingProviderAccountReadPolicy() {
        // GET /api/providers/me/requests was already covered by the Phase 2 broad
        // /api/providers/me/** GET rule - no new mapping needed for it.
        assertThat(resolver.resolve(request("GET", "/api/providers/me/requests")))
                .contains(RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_READ);
    }

    @Test
    void onboardingPostRoutesMapToMobileOnboardingWrite() {
        assertThat(resolver.resolve(request("POST", "/api/onboarding/choose-role")))
                .contains(RateLimitPolicy.MOBILE_ONBOARDING_WRITE);
        assertThat(resolver.resolve(request("POST", "/api/onboarding/provider-profile")))
                .contains(RateLimitPolicy.MOBILE_ONBOARDING_WRITE);
    }

    @Test
    void businessEventsPostRouteMapsToPublicBusinessEventWrite() {
        assertThat(resolver.resolve(request("POST", "/api/businesses/42/events")))
                .contains(RateLimitPolicy.PUBLIC_BUSINESS_EVENT_WRITE);
    }

    @Test
    void businessEventsWriteDoesNotClashWithPublicBusinessReadPolicy() {
        assertThat(resolver.resolve(request("GET", "/api/businesses/42")))
                .contains(RateLimitPolicy.PUBLIC_BUSINESS_READ);
        assertThat(resolver.resolve(request("POST", "/api/businesses/42/events")))
                .contains(RateLimitPolicy.PUBLIC_BUSINESS_EVENT_WRITE);
    }

    @Test
    void publicCompanyGetRoutesMapToPublicCompanyRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/companies")))
                .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/companies/42")))
                .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/companies/42/projects")))
                .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/company-projects/7")))
                .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
    }

    @Test
    void getCompanySlugDetailMapsToPublicCompanyRead() {
        // Phase 7B-G: the new canonical slug-based detail lookup falls under the same
        // existing GET /api/public/companies/** -> PUBLIC_COMPANY_READ wildcard rule already
        // covering the numeric-id and paginated-list routes above - no new Route entry was
        // needed (same reasoning as Builder's own slug route, Phase 6B-G).
        assertThat(resolver.resolve(request("GET", "/api/public/companies/slug/meridian-architects")))
                .contains(RateLimitPolicy.PUBLIC_COMPANY_READ);
    }

    @Test
    void publicArchitectDesignerGetRouteMapsToPublicArchitectDesignerRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/architect-designers/42")))
                .contains(RateLimitPolicy.PUBLIC_ARCHITECT_DESIGNER_READ);
    }

    @Test
    void newArchitectDesignerListAndSlugDetailRoutesMapToPublicArchitectDesignerReadNotPublicCompanyRead() {
        // Phase 8A-G (GAP-037/GAP-003B): both new routes fall under the same
        // existing GET /api/public/architect-designers/** -> PUBLIC_ARCHITECT_DESIGNER_READ
        // wildcard rule already covering the numeric-id route above - no new
        // Route entry was needed (same reasoning as Company's own slug route,
        // Phase 7B-G). Storage uses CompanyEntity, but that must not resolve
        // these routes to PUBLIC_COMPANY_READ - they are a distinct policy.
        assertThat(resolver.resolve(request("GET", "/api/public/architect-designers")))
                .contains(RateLimitPolicy.PUBLIC_ARCHITECT_DESIGNER_READ);
        assertThat(resolver.resolve(request("GET", "/api/public/architect-designers/slug/meridian-architects")))
                .contains(RateLimitPolicy.PUBLIC_ARCHITECT_DESIGNER_READ);
    }

    @Test
    void publicInstagramReelsGetRouteMapsToPublicInstagramReelsRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/instagram-reels")))
                .contains(RateLimitPolicy.PUBLIC_INSTAGRAM_REELS_READ);
    }

    @Test
    void publicProjectMeterGetRouteMapsToPublicProjectMeterRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/project-meter/cards")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_METER_READ);
    }

    @Test
    void projectScopedMeterRoutesAreAlreadyCoveredByPublicProjectRead() {
        // Distinct from /api/public/project-meter/** - these are under /api/projects/**
        // and were already covered before this phase.
        assertThat(resolver.resolve(request("GET", "/api/projects/42/meter")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_READ);
        assertThat(resolver.resolve(request("GET", "/api/projects/42/meter-summary")))
                .contains(RateLimitPolicy.PUBLIC_PROJECT_READ);
    }

    @Test
    void publicFeedGetRouteMapsToPublicFeedRead() {
        assertThat(resolver.resolve(request("GET", "/api/public/feed")))
                .contains(RateLimitPolicy.PUBLIC_FEED_READ);
    }

    @Test
    void writeMethodsDoNotAccidentallyMapToPhase3ReadPolicies() {
        assertThat(resolver.resolve(request("POST", "/api/public/companies"))).isEmpty();
        assertThat(resolver.resolve(request("PUT", "/api/public/architect-designers/1"))).isEmpty();
        assertThat(resolver.resolve(request("DELETE", "/api/public/instagram-reels"))).isEmpty();
        assertThat(resolver.resolve(request("PATCH", "/api/public/project-meter/cards"))).isEmpty();
        assertThat(resolver.resolve(request("POST", "/api/public/feed"))).isEmpty();
    }

    @Test
    void dashboardAndAdminPathsNeverMapToPhase3Policies() {
        assertThat(resolver.resolve(request("POST", "/api/dashboard/customer/requests"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/dashboard/public/companies"))).isEmpty();
        assertThat(resolver.resolve(request("POST", "/api/admin/onboarding/choose-role"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/admin/public/feed"))).isEmpty();
    }

    // ── Phase 4: final remaining mobile/public read route policies ──────────────

    @Test
    void brandGetRoutesMapToPublicBrandRead() {
        assertThat(resolver.resolve(request("GET", "/api/brands")))
                .contains(RateLimitPolicy.PUBLIC_BRAND_READ);
        assertThat(resolver.resolve(request("GET", "/api/brands/1")))
                .contains(RateLimitPolicy.PUBLIC_BRAND_READ);
        assertThat(resolver.resolve(request("GET", "/api/brands/1/promo")))
                .contains(RateLimitPolicy.PUBLIC_BRAND_READ);
        assertThat(resolver.resolve(request("GET", "/api/brands/1/page")))
                .contains(RateLimitPolicy.PUBLIC_BRAND_READ);
    }

    @Test
    void brandDistributorsRouteTakesPriorityOverPublicBrandReadPolicy() {
        // Specific must win: without correct ordering this would fall through to
        // the broad PUBLIC_BRAND_READ rule for /api/brands/**.
        assertThat(resolver.resolve(request("GET", "/api/brands/123/distributors")))
                .contains(RateLimitPolicy.PUBLIC_DISTRIBUTOR_READ);
        // Regression: other brand reads are unaffected.
        assertThat(resolver.resolve(request("GET", "/api/brands/123/promo")))
                .contains(RateLimitPolicy.PUBLIC_BRAND_READ);
    }

    @Test
    void distributorGetRoutesMapToPublicDistributorRead() {
        assertThat(resolver.resolve(request("GET", "/api/distributors")))
                .contains(RateLimitPolicy.PUBLIC_DISTRIBUTOR_READ);
        assertThat(resolver.resolve(request("GET", "/api/distributors/1")))
                .contains(RateLimitPolicy.PUBLIC_DISTRIBUTOR_READ);
        assertThat(resolver.resolve(request("GET", "/api/distributors/1/profile")))
                .contains(RateLimitPolicy.PUBLIC_DISTRIBUTOR_READ);
    }

    @Test
    void categoryGetRoutesMapToPublicCategoryRead() {
        assertThat(resolver.resolve(request("GET", "/api/categories")))
                .contains(RateLimitPolicy.PUBLIC_CATEGORY_READ);
        assertThat(resolver.resolve(request("GET", "/api/categories/blocks")))
                .contains(RateLimitPolicy.PUBLIC_CATEGORY_READ);
        assertThat(resolver.resolve(request("GET", "/api/categories/root")))
                .contains(RateLimitPolicy.PUBLIC_CATEGORY_READ);
        assertThat(resolver.resolve(request("GET", "/api/categories/5/children")))
                .contains(RateLimitPolicy.PUBLIC_CATEGORY_READ);
    }

    @Test
    void categoryBannersRouteMapsToPublicCategoryRead() {
        assertThat(resolver.resolve(request("GET", "/api/categories/123/banners")))
                .contains(RateLimitPolicy.PUBLIC_CATEGORY_READ);
    }

    @Test
    void trendingCityRouteReusesPublicCityReadPolicy() {
        assertThat(resolver.resolve(request("GET", "/api/public/cities/trending")))
                .contains(RateLimitPolicy.PUBLIC_CITY_READ);
        // Regression: the existing /api/cities/** rule is unaffected and distinct.
        assertThat(resolver.resolve(request("GET", "/api/cities")))
                .contains(RateLimitPolicy.PUBLIC_CITY_READ);
    }

    @Test
    void contentVersionRouteMapsToPublicContentVersionRead() {
        assertThat(resolver.resolve(request("GET", "/api/content/version")))
                .contains(RateLimitPolicy.PUBLIC_CONTENT_VERSION_READ);
    }

    @Test
    void sessionMeRouteMapsToMobileSessionRead() {
        assertThat(resolver.resolve(request("GET", "/api/session/me")))
                .contains(RateLimitPolicy.MOBILE_SESSION_READ);
    }

    @Test
    void writeMethodsDoNotAccidentallyMapToPhase4ReadPolicies() {
        assertThat(resolver.resolve(request("POST", "/api/brands"))).isEmpty();
        assertThat(resolver.resolve(request("PUT", "/api/distributors/1"))).isEmpty();
        assertThat(resolver.resolve(request("DELETE", "/api/categories/1"))).isEmpty();
        assertThat(resolver.resolve(request("PATCH", "/api/public/cities/trending"))).isEmpty();
        assertThat(resolver.resolve(request("POST", "/api/content/version"))).isEmpty();
        assertThat(resolver.resolve(request("POST", "/api/session/me"))).isEmpty();
    }

    @Test
    void dashboardAndAdminPathsNeverMapToPhase4Policies() {
        assertThat(resolver.resolve(request("GET", "/api/dashboard/brands"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/dashboard/categories/1/banners"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/admin/distributors"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/api/admin/session/me"))).isEmpty();
    }
}
