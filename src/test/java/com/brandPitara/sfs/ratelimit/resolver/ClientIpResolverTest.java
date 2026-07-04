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
}
