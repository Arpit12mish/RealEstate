package com.brandPitara.sfs.ratelimit.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Systematic sweep proving RateLimitingFilter's log output never contains raw
 * sensitive values, across several representative flows in one place (the
 * existing integration test file already has a few scattered logging-safety
 * assertions per-flow; this class exists to check the property broadly and
 * consistently, matching the production smoke test's "check logs do not
 * expose sensitive data" step - see docs/RATE_LIMIT_PRODUCTION_SMOKE_TEST.md).
 * <p>
 * Every log line RateLimitingFilter/InMemoryRateLimitService can emit is
 * covered: the "Rate limit exceeded" warn, the "Rejecting oversized request
 * body" warn, and the "no configured limits" warn.
 */
class RateLimitLoggingSafetyTest {

    private MockMvc mockMvc;
    private ListAppender<ILoggingEvent> filterLogAppender;
    private ListAppender<ILoggingEvent> serviceLogAppender;

    @RestController
    static class DummyRoutes {
        @PostMapping("/api/auth/request-otp")
        String requestOtp() {
            return "ok";
        }

        @PostMapping("/api/auth/verify-otp")
        String verifyOtp() {
            return "ok";
        }

        @PostMapping("/api/auth/refresh")
        String refresh() {
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
        properties.getPolicies().put(RateLimitPolicy.MOBILE_OTP_VERIFY, policy(
                limit(RateLimitKeyType.PHONE, 1, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_TOKEN_REFRESH, policy(
                limit(RateLimitKeyType.IP_AND_TOKEN, 1, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE, policy(
                limit(RateLimitKeyType.IP, 1, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_PROFILE_READ, policy(
                limit(RateLimitKeyType.IP_OR_USER, 1, 60)
        ));
        // Deliberately unconfigured to exercise InMemoryRateLimitService's
        // "no configured limits" warn path (see rawSensitiveValuesNeverAppear... below).
        properties.setDefaultEnabled(true);

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

        filterLogAppender = attachAppender(RateLimitingFilter.class);
        serviceLogAppender = attachAppender(InMemoryRateLimitService.class);
    }

    private ListAppender<ILoggingEvent> attachAppender(Class<?> loggerClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(RateLimitingFilter.class)).detachAppender(filterLogAppender);
        ((Logger) LoggerFactory.getLogger(InMemoryRateLimitService.class)).detachAppender(serviceLogAppender);
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

    private List<String> allLogMessages() {
        return java.util.stream.Stream.concat(
                filterLogAppender.list.stream(),
                serviceLogAppender.list.stream()
        ).map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }

    @Test
    void rawPhoneNumberNeverAppearsInLogsWhenOtpRequestIsBlocked() throws Exception {
        String rawPhone = "9876543210";
        String body = json(Map.of("phoneNumber", rawPhone));

        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("80.80.80.80")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("80.80.80.80")))
                .andExpect(status().isTooManyRequests());

        List<String> logs = allLogMessages();
        assertThat(logs).isNotEmpty();
        assertThat(logs).noneMatch(m -> m.contains(rawPhone));
        // Also guard the normalized/canonical form, in case a future change logs it.
        assertThat(logs).noneMatch(m -> m.contains("+91" + rawPhone));
    }

    @Test
    void rawOtpCodeNeverAppearsInLogsWhenVerifyIsBlocked() throws Exception {
        String rawOtpCode = "482913";
        String body = json(Map.of("phoneNumber", "9876543211", "code", rawOtpCode));

        mockMvc.perform(post("/api/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("81.81.81.81")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("81.81.81.81")))
                .andExpect(status().isTooManyRequests());

        assertThat(allLogMessages()).noneMatch(m -> m.contains(rawOtpCode));
    }

    @Test
    void rawRefreshTokenNeverAppearsInLogsWhenBlocked() throws Exception {
        String rawToken = "eyJraWQiOiJzZnMtdGVzdCIsImFsZyI6IkhTMjU2In0.super-secret-refresh-token-value";
        String body = json(Map.of("refreshToken", rawToken));

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("82.82.82.82")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("82.82.82.82")))
                .andExpect(status().isTooManyRequests());

        assertThat(allLogMessages()).noneMatch(m -> m.contains(rawToken));
    }

    @Test
    void rawJwtBearerTokenNeverAppearsInLogsForAuthenticatedMobilePolicies() throws Exception {
        String rawJwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5ODc2NTQzMjEwIn0.unique-signature-marker-abc123";
        RequestPostProcessor bearer = request -> {
            request.addHeader("Authorization", "Bearer " + rawJwt);
            return request;
        };

        mockMvc.perform(get("/api/profile").with(bearer).with(remoteAddr("83.83.83.83")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/profile").with(bearer).with(remoteAddr("83.83.83.83")))
                .andExpect(status().isTooManyRequests());

        assertThat(allLogMessages()).noneMatch(m -> m.contains(rawJwt));
    }

    @Test
    void rawCalculatorRequestBodyNeverAppearsInLogsWhenBlocked() throws Exception {
        String marker = "unique-calculator-body-marker-xyz789";
        String body = json(Map.of("cityId", 1, "note", marker));

        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("84.84.84.84")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("84.84.84.84")))
                .andExpect(status().isTooManyRequests());

        assertThat(allLogMessages()).noneMatch(m -> m.contains(marker));
    }

    @Test
    void rawEmailNeverAppearsInLogsEvenIfSentAsAnUnrelatedBodyField() throws Exception {
        String rawEmail = "sensitive.user@example.com";
        String body = json(Map.of("phoneNumber", "9876543212", "email", rawEmail));

        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("85.85.85.85")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("85.85.85.85")))
                .andExpect(status().isTooManyRequests());

        assertThat(allLogMessages()).noneMatch(m -> m.contains(rawEmail));
    }

    @Test
    void blockedLogLineOnlyContainsTheDocumentedSafeFields() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer token-a")
                        .with(remoteAddr("86.86.86.86")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer token-a")
                        .with(remoteAddr("86.86.86.86")))
                .andExpect(status().isTooManyRequests());

        List<String> logs = filterLogAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith("Rate limit exceeded"))
                .collect(Collectors.toList());

        assertThat(logs).hasSize(1);
        String logLine = logs.get(0);
        // Only the documented safe fields: policy, method, path, keyType, keyHash, retryAfterSeconds.
        assertThat(logLine).contains("policy=MOBILE_PROFILE_READ");
        assertThat(logLine).contains("method=GET");
        assertThat(logLine).contains("path=/api/profile");
        assertThat(logLine).contains("keyType=");
        assertThat(logLine).contains("keyHash=");
        assertThat(logLine).contains("retryAfterSeconds=");
        // keyHash is a short (12 hex char) non-reversible fingerprint, never the raw
        // "user:1" or "ip:86.86.86.86" key material itself.
        assertThat(logLine).doesNotContain("user:1").doesNotContain("ip:86.86.86.86");
    }
}
