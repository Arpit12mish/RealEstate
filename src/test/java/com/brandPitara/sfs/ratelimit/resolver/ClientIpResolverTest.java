package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {

    @Test
    void trustsForwardedForFromTrustedProxy() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustedProxies(java.util.List.of("127.0.0.1", "10.0.0.1"));
        ClientIpResolver resolver = new ClientIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void ignoresForwardedForWhenPeerIsNotATrustedProxy() {
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void fallsBackToRemoteAddrWhenNoForwardedForHeaderPresent() {
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void untrustedRemoteWithSpoofedForwardedForHeaderIsIgnored() {
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        // An attacker connecting directly to the app (not through the trusted proxy)
        // sends a forged header trying to impersonate someone else's IP.
        when(request.getRemoteAddr()).thenReturn("198.51.100.7");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void malformedForwardedForHeaderFallsBackSafelyWithoutThrowing() {
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(",,,");

        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void forwardedForWithOnlyWhitespaceSegmentsFallsBackSafely() {
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ,   ,   ");

        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void multipleForwardedForValuesWithLeadingBlankSegmentsChoosesFirstValidOne() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustedProxies(java.util.List.of("127.0.0.1", "10.0.0.1"));
        ClientIpResolver resolver = new ClientIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(" , 203.0.113.5, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    void trustedProxyListIsConfigurable() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustedProxies(java.util.List.of("10.20.30.40"));
        ClientIpResolver resolver = new ClientIpResolver(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.20.30.40");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");

        // The default loopback trust no longer applies once the list is overridden.
        HttpServletRequest loopbackRequest = mock(HttpServletRequest.class);
        when(loopbackRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(loopbackRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");

        assertThat(resolver.resolve(loopbackRequest)).isEqualTo("127.0.0.1");
    }

    @Test
    void canonicalizesIpv4AndEquivalentIpv6Forms() {
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        assertThat(resolver.normalize("203.000.113.005")).isEqualTo("203.0.113.5");
        assertThat(resolver.normalize("2001:0db8:0:0:0:0:0:1"))
                .isEqualTo(resolver.normalize("2001:db8::1"));
        assertThat(resolver.normalize("[2001:db8::1]")).isEqualTo(resolver.normalize("2001:db8::1"));
    }

    @Test
    void rejectsMalformedTrustedForwardedValuesAndFallsBackToProxyPeer() {
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("not-an-ip, 999.2.3.4");

        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void malformedButCharacterValidIpv6LookingInputIsRejectedWithoutAttemptingDnsResolution() {
        // "aaaa:bbbb" passes the old loose "[0-9a-f:.]+" character-class check but is not a
        // valid IPv6 literal (only 2 of 8 groups, no "::"). Before the fix this fell through to
        // InetAddress.getByName(candidate), which resolves it as a HOSTNAME - a real, blocking,
        // attacker-triggerable DNS lookup on the request thread. It must now be rejected by
        // syntax alone, fast and deterministically (this test would routinely take
        // seconds-to-never if it ever reached actual name resolution).
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        assertThat(resolver.normalize("aaaa:bbbb")).isEqualTo("unknown");
        assertThat(resolver.normalize("not:a:valid:ipv6:address:at:all:seriously:no")).isEqualTo("unknown");
        assertThat(resolver.normalize("dead:beef")).isEqualTo("unknown");
    }

    @Test
    void malformedIpv6LookingForwardedForHopIsSkippedRatherThanTrusted() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustedProxies(java.util.List.of("127.0.0.1"));
        ClientIpResolver resolver = new ClientIpResolver(properties);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("aaaa:bbbb");

        // The malformed hop is dropped (not a valid literal), so resolve() falls back to the
        // trusted peer itself rather than ever calling getByName on the malformed value.
        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void acceptsWellFormedIpv6LiteralsInVariousCompressedAndFullForms() {
        RateLimitProperties properties = new RateLimitProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        assertThat(resolver.normalize("::1")).isNotEqualTo("unknown");
        assertThat(resolver.normalize("fe80::1")).isNotEqualTo("unknown");
        assertThat(resolver.normalize("2001:db8:85a3:8d3:1319:8a2e:370:7348")).isNotEqualTo("unknown");
        assertThat(resolver.normalize("::ffff:192.0.2.1")).isNotEqualTo("unknown");
    }

    @Test
    void skipsConfiguredTrustedHopsFromRightAndIgnoresSpoofedLeftPrefix() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustedProxies(java.util.List.of("127.0.0.1", "10.0.0.1"));
        ClientIpResolver resolver = new ClientIpResolver(properties);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For"))
                .thenReturn("1.1.1.1, 203.0.113.88, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.88");
    }
}
