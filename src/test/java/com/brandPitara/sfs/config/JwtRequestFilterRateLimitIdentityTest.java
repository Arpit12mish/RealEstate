package com.brandPitara.sfs.config;

import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import com.brandPitara.sfs.util.JwtTokenUtil;
import com.brandPitara.sfs.util.ValidatedJwtClaims;
import com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot;
import com.brandPitara.sfs.enums.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtRequestFilterRateLimitIdentityTest {

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void validUserPublishesOnlyStableUserIdentity() throws Exception {
        JwtTokenUtil jwt = mock(JwtTokenUtil.class);
        UserDetails details = new MobileAuthenticationUserSnapshot(42L, "user@example.test", Role.CUSTOMER, true);
        UserDetailsService users = username -> details;
        when(jwt.parseAndValidate("raw-user-token")).thenReturn(new ValidatedJwtClaims(
                "user@example.test", "USER", 42L, null, null, Instant.now().plusSeconds(60)));
        MockHttpServletRequest request = request("Bearer raw-user-token");
        FilterChain chain = mock(FilterChain.class);

        new JwtRequestFilter(users, jwt, new LogSanitizer())
                .doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.AUTH_STATE))
                .isEqualTo(RateLimitAuthenticationAttributes.STATE_USER);
        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.USER_ID)).isEqualTo(42L);
        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.GUEST_SESSION_ID)).isNull();
        verify(chain).doFilter(any(), any());
        verify(jwt, times(1)).parseAndValidate("raw-user-token");
    }

    @Test
    void installationMetadataCannotImpersonateAnotherUser() throws Exception {
        JwtTokenUtil jwt = mock(JwtTokenUtil.class);
        UserDetails details = new MobileAuthenticationUserSnapshot(
                42L, "user@example.test", Role.CUSTOMER, true);
        UserDetailsService users = username -> details;
        when(jwt.parseAndValidate("raw-user-token")).thenReturn(new ValidatedJwtClaims(
                "user@example.test", "USER", 42L, null, "installation-claiming-user-99",
                Instant.now().plusSeconds(60)));
        MockHttpServletRequest request = request("Bearer raw-user-token");

        new JwtRequestFilter(users, jwt, new LogSanitizer())
                .doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.AUTH_STATE))
                .isEqualTo(RateLimitAuthenticationAttributes.STATE_USER);
        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.USER_ID)).isEqualTo(42L);
        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.GUEST_SESSION_ID)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isSameAs(details);
    }

    @Test
    void validGuestPublishesServerIssuedSessionIdNotInstallationId() throws Exception {
        JwtTokenUtil jwt = mock(JwtTokenUtil.class);
        when(jwt.parseAndValidate("raw-guest-token")).thenReturn(new ValidatedJwtClaims(
                "server-installation", "GUEST", null, 77L, "server-installation", Instant.now().plusSeconds(60)));
        MockHttpServletRequest request = request("Bearer raw-guest-token");

        new JwtRequestFilter(username -> { throw new AssertionError("no user lookup"); }, jwt, new LogSanitizer())
                .doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.AUTH_STATE))
                .isEqualTo(RateLimitAuthenticationAttributes.STATE_GUEST);
        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.GUEST_SESSION_ID)).isEqualTo(77L);
        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.USER_ID)).isNull();
    }

    @Test
    void invalidBearerPublishesFixedInvalidClassificationWithoutTokenMaterial() throws Exception {
        JwtTokenUtil jwt = mock(JwtTokenUtil.class);
        when(jwt.parseAndValidate("attacker-controlled-token"))
                .thenThrow(new IllegalArgumentException("invalid"));
        MockHttpServletRequest request = request("Bearer attacker-controlled-token");

        new JwtRequestFilter(username -> { throw new AssertionError("no user lookup"); }, jwt, new LogSanitizer())
                .doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.AUTH_STATE))
                .isEqualTo(RateLimitAuthenticationAttributes.STATE_INVALID);
        assertThat(request.getAttributeNames().asIterator()).toIterable()
                .noneMatch(name -> String.valueOf(request.getAttribute(name)).contains("attacker-controlled-token"));
    }

    @Test
    void jwtAndSnapshotUserIdMismatchIsRejected() throws Exception {
        JwtTokenUtil jwt = mock(JwtTokenUtil.class);
        when(jwt.parseAndValidate("mismatch-token")).thenReturn(new ValidatedJwtClaims(
                "user@example.test", "USER", 41L, null, null, Instant.now().plusSeconds(60)));
        UserDetailsService users = username -> new MobileAuthenticationUserSnapshot(
                42L, "user@example.test", Role.CUSTOMER, true);
        MockHttpServletRequest request = request("Bearer mismatch-token");

        new JwtRequestFilter(users, jwt, new LogSanitizer())
                .doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.AUTH_STATE))
                .isEqualTo(RateLimitAuthenticationAttributes.STATE_INVALID);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deterministicPublicV2IgnoresMalformedAuthorizationWithoutJwtParsing() throws Exception {
        JwtTokenUtil jwt = mock(JwtTokenUtil.class);
        JwtRequestFilter filter = new JwtRequestFilter(
                username -> { throw new AssertionError("no user lookup"); },
                jwt,
                new LogSanitizer()
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v2/public/projects/27");
        request.addHeader("Authorization", "definitely-not-a-bearer-token");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verifyNoInteractions(jwt);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(RateLimitAuthenticationAttributes.AUTH_STATE)).isNull();
    }

    @Test
    void deterministicPublicV2HeadAlsoIgnoresAuthorizationWithoutJwtParsing() throws Exception {
        JwtTokenUtil jwt = mock(JwtTokenUtil.class);
        JwtRequestFilter filter = new JwtRequestFilter(
                username -> { throw new AssertionError("no user lookup"); },
                jwt,
                new LogSanitizer()
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
                "HEAD", "/api/v2/public/projects/27");
        request.addHeader("Authorization", "malformed");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verifyNoInteractions(jwt);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/home");
        request.addHeader("Authorization", authorization);
        return request;
    }
}
