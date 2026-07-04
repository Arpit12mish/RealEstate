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
