package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;
import java.util.Optional;

/**
 * Maps an incoming request's HTTP method + path to the RateLimitPolicy that
 * governs it, so route-to-policy wiring lives in one place instead of being
 * scattered across controllers.
 */
@Component
public class RateLimitPolicyResolver {

    private record Route(HttpMethod method, String pattern, RateLimitPolicy policy) {
    }

    private final PathMatcher pathMatcher = new AntPathMatcher();

    // Order matters only where patterns could overlap for the same method; kept
    // most-specific-first as a defensive convention.
    private final List<Route> routes = List.of(
            new Route(HttpMethod.POST, "/api/auth/request-otp", RateLimitPolicy.MOBILE_OTP_REQUEST),
            new Route(HttpMethod.POST, "/api/auth/verify-otp", RateLimitPolicy.MOBILE_OTP_VERIFY),
            new Route(HttpMethod.POST, "/api/auth/refresh", RateLimitPolicy.MOBILE_TOKEN_REFRESH),
            new Route(HttpMethod.POST, "/api/auth/logout-all", RateLimitPolicy.MOBILE_LOGOUT_ALL),
            new Route(HttpMethod.POST, "/api/auth/logout", RateLimitPolicy.MOBILE_LOGOUT),
            new Route(HttpMethod.POST, "/api/auth/guest/session", RateLimitPolicy.MOBILE_GUEST_SESSION),
            new Route(HttpMethod.POST, "/api/projects/compare", RateLimitPolicy.PUBLIC_PROJECT_COMPARE),
            new Route(HttpMethod.POST, "/api/location/resolve", RateLimitPolicy.PUBLIC_LOCATION_RESOLVE),
            new Route(HttpMethod.GET, "/api/home/**", RateLimitPolicy.PUBLIC_HOME_READ),
            new Route(HttpMethod.GET, "/api/public/home/**", RateLimitPolicy.PUBLIC_HOME_READ),
            new Route(HttpMethod.GET, "/api/projects/**", RateLimitPolicy.PUBLIC_PROJECT_READ),
            new Route(HttpMethod.GET, "/api/public/search/**", RateLimitPolicy.PUBLIC_SEARCH),
            new Route(HttpMethod.GET, "/api/search/**", RateLimitPolicy.PUBLIC_SEARCH),

            // Phase 2: mobile action APIs - still action-based, authenticated,
            // write-heavy, or computationally expensive. Specific routes are listed
            // before the Phase 1.5 broad-prefix rules below wherever both could match
            // the same HTTP method (e.g. /api/providers/me/** vs /api/providers/**);
            // routes that only differ by HTTP method don't need ordering care since
            // resolve() filters by method first.
            new Route(HttpMethod.POST, "/api/public/stamp-duty/calculate", RateLimitPolicy.PUBLIC_CALCULATOR_WRITE),
            new Route(HttpMethod.POST, "/api/public/interior-cost/compare", RateLimitPolicy.PUBLIC_CALCULATOR_WRITE),
            new Route(HttpMethod.POST, "/api/public/interior-cost/compare-custom", RateLimitPolicy.PUBLIC_CALCULATOR_WRITE),
            new Route(HttpMethod.POST, "/api/public/circle-rates/calculate", RateLimitPolicy.PUBLIC_CALCULATOR_WRITE),

            new Route(HttpMethod.GET, "/api/profile", RateLimitPolicy.MOBILE_PROFILE_READ),
            new Route(HttpMethod.GET, "/api/profile/submitted-reviews", RateLimitPolicy.MOBILE_REVIEW_READ),
            new Route(HttpMethod.PUT, "/api/profile", RateLimitPolicy.MOBILE_PROFILE_WRITE),
            new Route(HttpMethod.PATCH, "/api/profile", RateLimitPolicy.MOBILE_PROFILE_WRITE),
            new Route(HttpMethod.POST, "/api/profile", RateLimitPolicy.MOBILE_PROFILE_WRITE),
            new Route(HttpMethod.POST, "/api/profile/photo/presign", RateLimitPolicy.MOBILE_MEDIA_OR_UPLOAD_ACTION),
            new Route(HttpMethod.POST, "/api/profile/photo/confirm", RateLimitPolicy.MOBILE_PROFILE_WRITE),
            new Route(HttpMethod.DELETE, "/api/profile/account", RateLimitPolicy.MOBILE_PROFILE_WRITE),

            new Route(HttpMethod.GET, "/api/project-favorites/*/exists", RateLimitPolicy.MOBILE_FAVORITE_READ),
            new Route(HttpMethod.GET, "/api/project-favorites", RateLimitPolicy.MOBILE_FAVORITE_READ),
            new Route(HttpMethod.POST, "/api/project-favorites/*/toggle", RateLimitPolicy.MOBILE_FAVORITE_WRITE),
            new Route(HttpMethod.DELETE, "/api/project-favorites/**", RateLimitPolicy.MOBILE_FAVORITE_WRITE),

            new Route(HttpMethod.POST, "/api/projects/*/reviews", RateLimitPolicy.MOBILE_REVIEW_WRITE),

            // Specific presign route must precede the broad /api/providers/me/** write
            // rule below (both are POST and would otherwise both match).
            new Route(HttpMethod.POST, "/api/providers/me/media/presign", RateLimitPolicy.MOBILE_MEDIA_OR_UPLOAD_ACTION),
            // Phase 3: same reasoning - this specific provider action must precede the
            // broad /api/providers/me/** write rule below (both POST, both would match).
            new Route(HttpMethod.POST, "/api/providers/me/requests/*/interest", RateLimitPolicy.MOBILE_PROVIDER_INTEREST_WRITE),
            // Specific /api/providers/me/** GET rule must precede the broader Phase 1.5
            // "GET /api/providers/**" -> PUBLIC_PROVIDER_READ rule below (both GET).
            new Route(HttpMethod.GET, "/api/providers/me/**", RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_READ),
            new Route(HttpMethod.POST, "/api/providers/me/**", RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE),
            new Route(HttpMethod.PUT, "/api/providers/me/**", RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE),
            new Route(HttpMethod.PATCH, "/api/providers/me/**", RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE),
            new Route(HttpMethod.DELETE, "/api/providers/me/**", RateLimitPolicy.MOBILE_PROVIDER_ACCOUNT_WRITE),

            // Phase 3: remaining mobile/public API rate limiting.
            new Route(HttpMethod.POST, "/api/customer/requests/**", RateLimitPolicy.MOBILE_SERVICE_REQUEST_WRITE),
            new Route(HttpMethod.POST, "/api/onboarding/choose-role", RateLimitPolicy.MOBILE_ONBOARDING_WRITE),
            new Route(HttpMethod.POST, "/api/onboarding/provider-profile", RateLimitPolicy.MOBILE_ONBOARDING_WRITE),
            new Route(HttpMethod.POST, "/api/businesses/*/events", RateLimitPolicy.PUBLIC_BUSINESS_EVENT_WRITE),
            new Route(HttpMethod.GET, "/api/public/companies/**", RateLimitPolicy.PUBLIC_COMPANY_READ),
            new Route(HttpMethod.GET, "/api/public/company-projects/**", RateLimitPolicy.PUBLIC_COMPANY_READ),
            new Route(HttpMethod.GET, "/api/public/architect-designers/**", RateLimitPolicy.PUBLIC_ARCHITECT_DESIGNER_READ),
            new Route(HttpMethod.GET, "/api/public/instagram-reels/**", RateLimitPolicy.PUBLIC_INSTAGRAM_REELS_READ),
            new Route(HttpMethod.GET, "/api/public/project-meter/**", RateLimitPolicy.PUBLIC_PROJECT_METER_READ),
            new Route(HttpMethod.GET, "/api/public/feed/**", RateLimitPolicy.PUBLIC_FEED_READ),

            // Phase 4: final remaining mobile/public read route policies.
            // Specific distributor-under-brand route must precede the broad
            // /api/brands/** rule below (both GET, both would otherwise match).
            new Route(HttpMethod.GET, "/api/brands/*/distributors", RateLimitPolicy.PUBLIC_DISTRIBUTOR_READ),
            new Route(HttpMethod.GET, "/api/brands/**", RateLimitPolicy.PUBLIC_BRAND_READ),
            new Route(HttpMethod.GET, "/api/distributors/**", RateLimitPolicy.PUBLIC_DISTRIBUTOR_READ),
            new Route(HttpMethod.GET, "/api/categories/**", RateLimitPolicy.PUBLIC_CATEGORY_READ),
            // Distinct prefix from the existing /api/cities/** rule below - reuses
            // PUBLIC_CITY_READ rather than adding a new policy (semantically a city read).
            new Route(HttpMethod.GET, "/api/public/cities/trending", RateLimitPolicy.PUBLIC_CITY_READ),
            new Route(HttpMethod.GET, "/api/content/version", RateLimitPolicy.PUBLIC_CONTENT_VERSION_READ),
            new Route(HttpMethod.GET, "/api/session/me", RateLimitPolicy.MOBILE_SESSION_READ),

            // Phase 1.5: remaining public mobile/public read APIs.
            new Route(HttpMethod.GET, "/api/cities/**", RateLimitPolicy.PUBLIC_CITY_READ),
            new Route(HttpMethod.GET, "/api/builders/**", RateLimitPolicy.PUBLIC_BUILDER_READ),
            new Route(HttpMethod.GET, "/api/businesses/**", RateLimitPolicy.PUBLIC_BUSINESS_READ),
            new Route(HttpMethod.GET, "/api/providers/**", RateLimitPolicy.PUBLIC_PROVIDER_READ),
            new Route(HttpMethod.GET, "/api/app-content/**", RateLimitPolicy.PUBLIC_APP_CONTENT_READ),
            new Route(HttpMethod.GET, "/api/app/screen-content", RateLimitPolicy.PUBLIC_APP_CONTENT_READ),
            new Route(HttpMethod.GET, "/api/public/stamp-duty/**", RateLimitPolicy.PUBLIC_CALCULATOR_READ),
            new Route(HttpMethod.GET, "/api/public/interior-cost/**", RateLimitPolicy.PUBLIC_CALCULATOR_READ),
            new Route(HttpMethod.GET, "/api/public/circle-rates/**", RateLimitPolicy.PUBLIC_CALCULATOR_READ),
            new Route(HttpMethod.GET, "/api/public/calculators/**", RateLimitPolicy.PUBLIC_CALCULATOR_READ)
    );

    public Optional<RateLimitPolicy> resolve(HttpServletRequest request) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        final String matchPath = path;

        return routes.stream()
                .filter(route -> route.method() == method)
                .filter(route -> pathMatcher.match(route.pattern(), matchPath))
                .map(Route::policy)
                .findFirst();
    }
}
