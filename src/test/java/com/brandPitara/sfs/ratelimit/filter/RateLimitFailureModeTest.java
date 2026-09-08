package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitFailureMode;
import com.brandPitara.sfs.ratelimit.enums.RateLimitIdentityType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.metrics.RateLimitMetrics;
import com.brandPitara.sfs.ratelimit.model.RateLimitIdentity;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitIdentityResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitKeyResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import com.brandPitara.sfs.ratelimit.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitFailureModeTest {

    @RestController
    static class Routes {
        @GetMapping("/api/home") String home() { return "ok"; }
        @PostMapping("/api/auth/request-otp") String otp() { return "ok"; }
    }

    @Test
    void publicReadFailsOpenWhenEnforcementFails() throws Exception {
        Fixture fixture = fixture(false);
        fixture.mockMvc.perform(get("/api/home")).andExpect(status().isOk());
        fixture.registry.get("sfs.rate_limit.failure")
                .tag("policy", "PUBLIC_HOME_READ").tag("mode", "FAIL_OPEN").counter();
    }

    @Test
    void authenticationEndpointFailsClosedWithStandardEnvelope() throws Exception {
        Fixture fixture = fixture(true);
        fixture.mockMvc.perform(post("/api/auth/request-otp")
                        .header("X-Request-Id", "rid-rate-failure")
                        .contentType("application/json")
                        .content("{\"phoneNumber\":\"9999999999\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_UNAVAILABLE"))
                .andExpect(jsonPath("$.requestId").value("rid-rate-failure"));
        fixture.registry.get("sfs.rate_limit.failure")
                .tag("policy", "MOBILE_OTP_REQUEST").tag("mode", "FAIL_CLOSED").counter();
    }

    private Fixture fixture(boolean failClosedOtp) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy());
        properties.getPolicies().put(RateLimitPolicy.MOBILE_OTP_REQUEST, policy());
        if (failClosedOtp) properties.getFailClosedPolicies().add(RateLimitPolicy.MOBILE_OTP_REQUEST);
        RateLimitService service = mock(RateLimitService.class);
        when(service.checkAndConsume(any(), any())).thenThrow(new IllegalStateException("synthetic failure"));
        ClientIpResolver ipResolver = new ClientIpResolver(properties);
        RateLimitIdentityResolver identityResolver = mock(RateLimitIdentityResolver.class);
        when(identityResolver.resolve(any(HttpServletRequest.class))).thenReturn(
                new RateLimitIdentity("anonymous:127.0.0.1", "127.0.0.1",
                        RateLimitIdentityType.ANONYMOUS, false));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitPolicyResolver(), new RateLimitKeyResolver(), ipResolver,
                identityResolver, service, properties, new ObjectMapper(), new RateLimitMetrics(registry));
        return new Fixture(MockMvcBuilders.standaloneSetup(new Routes()).addFilters(filter).build(), registry);
    }

    private RateLimitProperties.PolicyConfig policy() {
        RateLimitProperties.LimitConfig limit = new RateLimitProperties.LimitConfig();
        limit.setKeyType(RateLimitKeyType.IP);
        limit.setCapacity(10);
        limit.setRefillTokens(10);
        limit.setRefillPeriodSeconds(60);
        RateLimitProperties.PolicyConfig policy = new RateLimitProperties.PolicyConfig();
        policy.setFailureMode(RateLimitFailureMode.FAIL_OPEN);
        policy.setLimits(List.of(limit));
        return policy;
    }

    private record Fixture(MockMvc mockMvc, SimpleMeterRegistry registry) {}
}
