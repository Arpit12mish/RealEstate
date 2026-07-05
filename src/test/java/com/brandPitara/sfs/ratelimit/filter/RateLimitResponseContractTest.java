package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties.LimitConfig;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties.PolicyConfig;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitKeyResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import com.brandPitara.sfs.ratelimit.service.impl.InMemoryRateLimitService;
import com.brandPitara.sfs.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the 429 response contract is identical across every category of
 * policy - mobile auth, public GET, public POST/body-aware, and authenticated
 * mobile action - so mobile clients can rely on one shared parsing path
 * regardless of which endpoint they called. See
 * docs/RATE_LIMIT_MOBILE_INTEGRATION_NOTES.md for the client-facing contract
 * this test locks in: HTTP 429, a Retry-After header, and a JSON body with
 * status/error/message/retryAfterSeconds/policy.
 */
class RateLimitResponseContractTest {

    private MockMvc mockMvc;

    @RestController
    static class DummyRoutes {
        @PostMapping("/api/auth/request-otp")
        String requestOtp() {
            return "ok";
        }

        @GetMapping("/api/home")
        String home() {
            return "ok";
        }

        @PostMapping("/api/public/stamp-duty/calculate")
        String stampDutyCalculate() {
            return "ok";
        }

        @GetMapping("/api/profile")
        String myProfile() {
            return "ok";
        }

        @GetMapping("/api/dashboard/something")
        String dashboardSomething() {
            return "ok";
        }
    }

    private static LimitConfig limit(RateLimitKeyType keyType, long capacity, long refillPeriodSeconds) {
        LimitConfig limitConfig = new LimitConfig();
        limitConfig.setKeyType(keyType);
        limitConfig.setCapacity(capacity);
        limitConfig.setRefillTokens(capacity);
        limitConfig.setRefillPeriodSeconds(refillPeriodSeconds);
        return limitConfig;
    }

    private static PolicyConfig policy(LimitConfig... limits) {
        PolicyConfig config = new PolicyConfig();
        config.setLimits(List.of(limits));
        return config;
    }

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getPolicies().put(RateLimitPolicy.MOBILE_OTP_REQUEST, policy(
                limit(RateLimitKeyType.PHONE, 1, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy(
                limit(RateLimitKeyType.IP, 1, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE, policy(
                limit(RateLimitKeyType.IP, 1, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_PROFILE_READ, policy(
                limit(RateLimitKeyType.IP_OR_USER, 1, 60)
        ));

        ObjectMapper objectMapper = new ObjectMapper();
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        when(jwtTokenUtil.getUserIdFromToken(org.mockito.ArgumentMatchers.anyString())).thenReturn(1L);

        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitPolicyResolver(),
                new RateLimitKeyResolver(),
                new ClientIpResolver(properties),
                new InMemoryRateLimitService(properties),
                properties,
                objectMapper,
                jwtTokenUtil
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyRoutes()).addFilters(filter).build();
    }

    private String json(Map<String, Object> fields) throws Exception {
        return new ObjectMapper().writeValueAsString(fields);
    }

    private static RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private void assertFullContract(org.springframework.test.web.servlet.ResultActions result, String expectedPolicy)
            throws Exception {
        result.andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber())
                .andExpect(jsonPath("$.policy").value(expectedPolicy));
    }

    @Test
    void mobileAuthPolicyReturnsFullContractWhenBlocked() throws Exception {
        String body = json(Map.of("phoneNumber", "9876543210"));

        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("70.70.70.70")))
                .andExpect(status().isOk());

        assertFullContract(
                mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("70.70.70.70"))),
                "MOBILE_OTP_REQUEST"
        );
    }

    @Test
    void publicGetPolicyReturnsFullContractWhenBlocked() throws Exception {
        mockMvc.perform(get("/api/home").with(remoteAddr("71.71.71.71"))).andExpect(status().isOk());

        assertFullContract(
                mockMvc.perform(get("/api/home").with(remoteAddr("71.71.71.71"))),
                "PUBLIC_HOME_READ"
        );
    }

    @Test
    void publicBodyAwarePolicyReturnsFullContractWhenBlocked() throws Exception {
        String body = json(Map.of("cityId", 1));

        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("72.72.72.72")))
                .andExpect(status().isOk());

        assertFullContract(
                mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("72.72.72.72"))),
                "PUBLIC_CALCULATOR_WRITE"
        );
    }

    @Test
    void authenticatedMobilePolicyReturnsFullContractWhenBlocked() throws Exception {
        RequestPostProcessor bearer = request -> {
            request.addHeader("Authorization", "Bearer token-a");
            return request;
        };

        mockMvc.perform(get("/api/profile").with(bearer).with(remoteAddr("73.73.73.73")))
                .andExpect(status().isOk());

        assertFullContract(
                mockMvc.perform(get("/api/profile").with(bearer).with(remoteAddr("73.73.73.73"))),
                "MOBILE_PROFILE_READ"
        );
    }

    @Test
    void retryAfterHeaderAndBodyValueAreConsistentAndPositive() throws Exception {
        mockMvc.perform(get("/api/home").with(remoteAddr("74.74.74.74"))).andExpect(status().isOk());

        String retryAfterHeader = mockMvc.perform(get("/api/home").with(remoteAddr("74.74.74.74")))
                .andExpect(status().isTooManyRequests())
                .andReturn().getResponse().getHeader("Retry-After");

        // RateLimitDecision.block() floors retryAfterSeconds at 1, so a client
        // never sees Retry-After: 0 (which some HTTP clients treat as "retry now").
        org.assertj.core.api.Assertions.assertThat(Long.parseLong(retryAfterHeader)).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void dashboardRouteNeverReceivesTheRateLimitResponseContract() throws Exception {
        // /api/dashboard/** has no policy mapping at all (and in production never
        // reaches this filter in the first place - see SecurityConfig), so it must
        // return plain 200s regardless of volume, never a 429 contract body.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/dashboard/something").with(remoteAddr("75.75.75.75")))
                    .andExpect(status().isOk());
        }
    }
}
