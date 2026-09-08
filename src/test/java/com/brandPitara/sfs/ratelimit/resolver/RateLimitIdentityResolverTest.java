package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitIdentityType;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import com.brandPitara.sfs.ratelimit.model.RateLimitIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitIdentityResolverTest {

    private final RateLimitIdentityResolver resolver =
            new RateLimitIdentityResolver(new ClientIpResolver(new RateLimitProperties()));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenAuthenticatedUsersBehindOneNatHaveIndependentPrimaryIdentitiesAndOneAbuseIp() {
        Set<String> primary = new HashSet<>();
        Set<String> abuse = new HashSet<>();
        for (long userId = 1; userId <= 10; userId++) {
            MockHttpServletRequest request = request("203.0.113.10");
            request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE,
                    RateLimitAuthenticationAttributes.STATE_USER);
            request.setAttribute(RateLimitAuthenticationAttributes.USER_ID, userId);
            RateLimitIdentity identity = resolver.resolve(request);
            primary.add(identity.primaryKey());
            abuse.add(identity.abuseIp());
            assertThat(identity.type()).isEqualTo(RateLimitIdentityType.AUTHENTICATED_USER);
        }
        assertThat(primary).hasSize(10);
        assertThat(abuse).containsExactly("203.0.113.10");
    }

    @Test
    void tenValidatedGuestSessionsBehindOneNatHaveIndependentPrimaryIdentities() {
        Set<String> primary = new HashSet<>();
        for (long guestSessionId = 1; guestSessionId <= 10; guestSessionId++) {
            MockHttpServletRequest request = request("203.0.113.11");
            request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE,
                    RateLimitAuthenticationAttributes.STATE_GUEST);
            request.setAttribute(RateLimitAuthenticationAttributes.GUEST_SESSION_ID, guestSessionId);
            RateLimitIdentity identity = resolver.resolve(request);
            primary.add(identity.primaryKey());
            assertThat(identity.type()).isEqualTo(RateLimitIdentityType.VALID_GUEST);
            assertThat(identity.abuseIp()).isEqualTo("203.0.113.11");
        }
        assertThat(primary).hasSize(10);
    }

    @Test
    void oneAuthenticatedUserAcrossMultipleIpsKeepsOnePrimaryAndSeveralAbuseIdentities() {
        Set<String> primary = new HashSet<>();
        Set<String> abuse = new HashSet<>();
        for (int octet = 1; octet <= 3; octet++) {
            MockHttpServletRequest request = request("198.51.100." + octet);
            request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE,
                    RateLimitAuthenticationAttributes.STATE_USER);
            request.setAttribute(RateLimitAuthenticationAttributes.USER_ID, 42L);
            RateLimitIdentity identity = resolver.resolve(request);
            primary.add(identity.primaryKey());
            abuse.add(identity.abuseIp());
        }
        assertThat(primary).containsExactly("user:42");
        assertThat(abuse).hasSize(3);
    }

    @Test
    void thousandsOfDifferentInvalidBearerTokensCollapseToOneIpScopedClassification() {
        Set<String> primary = new HashSet<>();
        for (int index = 0; index < 10_000; index++) {
            MockHttpServletRequest request = request("192.0.2.25");
            request.addHeader("Authorization", "Bearer attacker-token-" + index);
            request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE,
                    RateLimitAuthenticationAttributes.STATE_INVALID);
            primary.add(resolver.resolve(request).primaryKey());
        }
        assertThat(primary).containsExactly("invalid-auth:192.0.2.25");
    }

    @Test
    void missingGuestSessionIdAndMalformedAuthorizationFallBackToInvalidAuthWithoutRawValues() {
        MockHttpServletRequest missingGuest = request("192.0.2.26");
        missingGuest.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE,
                RateLimitAuthenticationAttributes.STATE_GUEST);
        RateLimitIdentity missing = resolver.resolve(missingGuest);

        MockHttpServletRequest malformed = request("192.0.2.26");
        malformed.addHeader("Authorization", "Basic raw-secret-value");
        RateLimitIdentity invalid = resolver.resolve(malformed);

        assertThat(missing.type()).isEqualTo(RateLimitIdentityType.INVALID_AUTH);
        assertThat(invalid.primaryKey()).isEqualTo("invalid-auth:192.0.2.26")
                .doesNotContain("raw-secret-value");
    }

    @Test
    void arbitraryGuestPrincipalTextIsNeverUsedAsGuestIdentity() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "attacker-installation-id", null, Set.of(new SimpleGrantedAuthority("ROLE_GUEST"))));

        RateLimitIdentity identity = resolver.resolve(request("192.0.2.27"));

        assertThat(identity.type()).isEqualTo(RateLimitIdentityType.ANONYMOUS);
        assertThat(identity.primaryKey()).isEqualTo("anonymous:192.0.2.27")
                .doesNotContain("attacker-installation-id");
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/home");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
