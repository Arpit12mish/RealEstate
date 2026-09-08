package com.brandPitara.sfs.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;

/** Shared infrastructure-request exclusions for security-chain application filters. */
public final class SecurityRequestBypass {

    private SecurityRequestBypass() {
    }

    public static boolean shouldBypass(HttpServletRequest request) {
        DispatcherType dispatcherType = request.getDispatcherType();
        if (dispatcherType == DispatcherType.ERROR || dispatcherType == DispatcherType.ASYNC) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        if (path == null) return false;
        return "/error".equals(path)
                || "/api/health".equals(path)
                || path.startsWith("/api/health/")
                || "/actuator/health".equals(path)
                || path.startsWith("/actuator/health/");
    }
}
