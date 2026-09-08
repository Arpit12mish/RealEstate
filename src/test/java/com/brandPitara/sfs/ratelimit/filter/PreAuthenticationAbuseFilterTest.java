package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import com.brandPitara.sfs.ratelimit.metrics.RateLimitMetrics;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import com.brandPitara.sfs.ratelimit.service.impl.InMemoryRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

class PreAuthenticationAbuseFilterTest {

    @Test
    void bearerTrafficIsEventuallyBlockedBeforeDownstreamJwtParsing() throws Exception {
        RateLimitProperties properties = properties(1);
        PreAuthenticationAbuseFilter filter = filter(properties);
        FilterChain jwtStage = mock(FilterChain.class);

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(request("Bearer first"), first, jwtStage);
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(request("Bearer random-attacker-token"), second, jwtStage);
        MockHttpServletResponse third = new MockHttpServletResponse();
        filter.doFilter(request("Bearer another-random-token"), third, jwtStage);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(third.getHeader("Retry-After"))).isGreaterThanOrEqualTo(1);
        verify(jwtStage, times(2)).doFilter(any(), any());
    }

    @Test
    void successfulPreAuthConsumptionMarksRequestForPostAuthDeduplication() throws Exception {
        RateLimitProperties properties = properties(10);
        PreAuthenticationAbuseFilter filter = filter(properties);
        MockHttpServletRequest request = request("Bearer valid");
        FilterChain chain = (req, res) -> assertThat(req.getAttribute(
                RateLimitAuthenticationAttributes.IP_ABUSE_CONSUMED)).isEqualTo(Boolean.TRUE);

        filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    @Test
    void absentAuthorizationHeaderDoesNotConsumeAbuseBucket() throws Exception {
        RateLimitProperties properties = properties(1);
        InMemoryRateLimitService service = mock(InMemoryRateLimitService.class);
        PreAuthenticationAbuseFilter filter = new PreAuthenticationAbuseFilter(
                new RateLimitPolicyResolver(), new ClientIpResolver(properties), service,
                properties, RateLimitMetrics.isolated(), new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/home/cards");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        verifyNoInteractions(service);
    }

    @Test
    void deterministicPublicV2DoesNotTreatAuthorizationAsPreAuthenticationTraffic() throws Exception {
        RateLimitProperties properties = properties(1);
        InMemoryRateLimitService service = mock(InMemoryRateLimitService.class);
        PreAuthenticationAbuseFilter filter = new PreAuthenticationAbuseFilter(
                new RateLimitPolicyResolver(), new ClientIpResolver(properties), service,
                properties, RateLimitMetrics.isolated(), new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v2/public/projects/27");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("Authorization", "malformed");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verifyNoInteractions(service);
    }

    private PreAuthenticationAbuseFilter filter(RateLimitProperties properties) {
        return new PreAuthenticationAbuseFilter(new RateLimitPolicyResolver(), new ClientIpResolver(properties),
                new InMemoryRateLimitService(properties), properties, RateLimitMetrics.isolated(), new ObjectMapper());
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/home/cards");
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("Authorization", authorization);
        return request;
    }

    private RateLimitProperties properties(long capacity) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAbuseCapacityMultiplier(2);
        RateLimitProperties.LimitConfig limit = new RateLimitProperties.LimitConfig();
        limit.setKeyType(RateLimitKeyType.PRIMARY_IDENTITY);
        limit.setCapacity(capacity);
        limit.setRefillTokens(capacity);
        limit.setRefillPeriodSeconds(60);
        RateLimitProperties.PolicyConfig policy = new RateLimitProperties.PolicyConfig();
        policy.setLimits(java.util.List.of(limit));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy);
        return properties;
    }
}
