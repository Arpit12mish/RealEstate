package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import com.brandPitara.sfs.ratelimit.metrics.RateLimitMetrics;
import com.brandPitara.sfs.ratelimit.model.RateLimitDecision;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitIdentityResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitKeyResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import com.brandPitara.sfs.ratelimit.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitDoubleConsumptionTest {

    @Test
    void postAuthLimiterOmitsIpAbuseAfterPreAuthAlreadyConsumedIt() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        RateLimitProperties.LimitConfig limit = new RateLimitProperties.LimitConfig();
        limit.setKeyType(RateLimitKeyType.PRIMARY_IDENTITY);
        limit.setCapacity(10); limit.setRefillTokens(10); limit.setRefillPeriodSeconds(60);
        RateLimitProperties.PolicyConfig config = new RateLimitProperties.PolicyConfig();
        config.setLimits(List.of(limit));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, config);
        RateLimitPolicyResolver policies = mock(RateLimitPolicyResolver.class);
        when(policies.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(RateLimitPolicy.PUBLIC_HOME_READ));
        RateLimitService service = mock(RateLimitService.class);
        when(service.checkAndConsume(eq(RateLimitPolicy.PUBLIC_HOME_READ), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(RateLimitDecision.allow(RateLimitPolicy.PUBLIC_HOME_READ));
        ClientIpResolver ip = new ClientIpResolver(properties);
        RateLimitingFilter filter = new RateLimitingFilter(policies, new RateLimitKeyResolver(), ip,
                new RateLimitIdentityResolver(ip), service, properties, new ObjectMapper(), RateLimitMetrics.isolated());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/home/cards");
        request.setRemoteAddr("203.0.113.20");
        request.addHeader("Authorization", "Bearer validated-upstream");
        request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE, RateLimitAuthenticationAttributes.STATE_USER);
        request.setAttribute(RateLimitAuthenticationAttributes.USER_ID, 42L);
        request.setAttribute(RateLimitAuthenticationAttributes.IP_ABUSE_CONSUMED, Boolean.TRUE);

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<RateLimitKeyType, String>> keys = ArgumentCaptor.forClass(Map.class);
        verify(service).checkAndConsume(eq(RateLimitPolicy.PUBLIC_HOME_READ), keys.capture());
        assertThat(keys.getValue()).containsEntry(RateLimitKeyType.PRIMARY_IDENTITY, "user:42")
                .doesNotContainKey(RateLimitKeyType.IP_ABUSE);
    }
}
