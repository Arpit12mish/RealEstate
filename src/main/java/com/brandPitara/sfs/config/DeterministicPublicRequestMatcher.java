package com.brandPitara.sfs.config;

import jakarta.servlet.http.HttpServletRequest;

/** Paths whose representation and request identity must remain viewer-independent. */
public final class DeterministicPublicRequestMatcher {

    private static final String PUBLIC_V2_PREFIX = "/api/v2/public/";

    private DeterministicPublicRequestMatcher() {
    }

    public static boolean matches(HttpServletRequest request) {
        if (request == null || !("GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod()))) {
            return false;
        }
        String path = request.getRequestURI();
        return path != null && path.startsWith(PUBLIC_V2_PREFIX);
    }
}
