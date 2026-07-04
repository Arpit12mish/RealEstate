package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the client IP for rate-limiting purposes. X-Forwarded-For is only
 * trusted when the direct TCP peer is a configured trusted proxy (default:
 * loopback, matching a single-EC2-instance deployment with a local nginx in
 * front of the app) - otherwise it falls back to request.getRemoteAddr(), so
 * a request cannot spoof its rate-limit identity by sending an arbitrary
 * X-Forwarded-For header directly to the app.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final RateLimitProperties properties;

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
            String firstHop = firstHop(forwardedFor);
            if (firstHop != null) {
                return firstHop;
            }
        }

        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }
        return properties.getTrustedProxies().contains(remoteAddr);
    }

    /**
     * Returns the first non-blank hop in a comma-separated X-Forwarded-For value,
     * or null if there isn't one. Uses a limit of -1 so a malformed value (e.g.
     * all-commas, leading/trailing commas, empty segments) never produces a
     * zero-length array - String.split with the default limit of 0 strips
     * trailing empty strings and can return an empty array for input like ",,,",
     * which would otherwise throw ArrayIndexOutOfBoundsException here.
     */
    private String firstHop(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        for (String candidate : forwardedFor.split(",", -1)) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }
}
