package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.config.DeterministicPublicRequestMatcher;
import com.brandPitara.sfs.ratelimit.enums.RateLimitIdentityType;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import com.brandPitara.sfs.ratelimit.model.RateLimitIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves one bounded primary identity plus an independent IP-abuse identity.
 * Raw bearer tokens, device IDs and unvalidated guest installation IDs are
 * never identity material.
 */
@Component
public class RateLimitIdentityResolver {

    private final ClientIpResolver clientIpResolver;

    public RateLimitIdentityResolver(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    public RateLimitIdentity resolve(HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);
        if (DeterministicPublicRequestMatcher.matches(request)) {
            return new RateLimitIdentity("anonymous:" + ip, ip, RateLimitIdentityType.ANONYMOUS, false);
        }
        Object state = request.getAttribute(RateLimitAuthenticationAttributes.AUTH_STATE);

        if (RateLimitAuthenticationAttributes.STATE_USER.equals(state)) {
            Long userId = longAttribute(request, RateLimitAuthenticationAttributes.USER_ID);
            if (userId != null) return user(userId, ip, false);
            return invalid(ip, true);
        }
        if (RateLimitAuthenticationAttributes.STATE_GUEST.equals(state)) {
            Long guestSessionId = longAttribute(request, RateLimitAuthenticationAttributes.GUEST_SESSION_ID);
            if (guestSessionId != null) return guest("id:" + guestSessionId, ip, false);
            return invalid(ip, true);
        }
        if (RateLimitAuthenticationAttributes.STATE_INVALID.equals(state)) {
            return invalid(ip, false);
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            return invalid(ip, true);
        }

        return new RateLimitIdentity("anonymous:" + ip, ip, RateLimitIdentityType.ANONYMOUS, false);
    }

    private RateLimitIdentity user(Long userId, String ip, boolean fallback) {
        return new RateLimitIdentity("user:" + userId, ip, RateLimitIdentityType.AUTHENTICATED_USER, fallback);
    }

    private RateLimitIdentity guest(String stableGuestId, String ip, boolean fallback) {
        return new RateLimitIdentity("guest:" + stableGuestId, ip, RateLimitIdentityType.VALID_GUEST, fallback);
    }

    private RateLimitIdentity invalid(String ip, boolean fallback) {
        return new RateLimitIdentity("invalid-auth:" + ip, ip, RateLimitIdentityType.INVALID_AUTH, fallback);
    }

    private Long longAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        if (value instanceof Long number) return number;
        if (value instanceof Number number) return number.longValue();
        return null;
    }
}
