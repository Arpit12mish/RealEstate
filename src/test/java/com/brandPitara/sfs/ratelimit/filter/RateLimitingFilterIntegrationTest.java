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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of RateLimitingFilter wired the same way SecurityConfig
 * wires it, against small dummy controllers at the exact production routes.
 * Limits are intentionally tiny so each test hits its threshold in a handful
 * of requests.
 */
class RateLimitingFilterIntegrationTest {

    private MockMvc mockMvc;
    private ListAppender<ILoggingEvent> logAppender;

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

        @PostMapping("/api/auth/guest/session")
        String guestSession() {
            return "ok";
        }

        @GetMapping("/api/home")
        String home() {
            return "ok";
        }

        @RequestMapping(value = "/api/projects/{id}", method = org.springframework.web.bind.annotation.RequestMethod.GET)
        String projectDetail() {
            return "ok";
        }

        @PostMapping("/api/projects/compare")
        String compare() {
            return "ok";
        }

        @PostMapping("/api/location/resolve")
        String locationResolve() {
            return "ok";
        }

        // Phase 1.5 dummy routes.
        @GetMapping("/api/cities")
        String cities() {
            return "ok";
        }

        @PostMapping("/api/cities")
        String createCity() {
            return "ok";
        }

        @GetMapping("/api/builders/{id}")
        String builderDetail() {
            return "ok";
        }

        @PostMapping("/api/builders")
        String createBuilder() {
            return "ok";
        }

        @GetMapping("/api/businesses/{id}")
        String businessDetail() {
            return "ok";
        }

        @GetMapping("/api/providers/{id}")
        String providerDetail() {
            return "ok";
        }

        @GetMapping("/api/app-content/pages/{slug}")
        String appContentPage() {
            return "ok";
        }

        @GetMapping("/api/app/screen-content")
        String appScreenContent() {
            return "ok";
        }

        @PostMapping("/api/app/screen-content")
        String createAppScreenContent() {
            return "ok";
        }

        @GetMapping("/api/public/stamp-duty/cities")
        String stampDutyCities() {
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
                limit(RateLimitKeyType.PHONE, 2, 60),
                limit(RateLimitKeyType.IP, 3, 3600)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_OTP_VERIFY, policy(
                limit(RateLimitKeyType.PHONE, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_TOKEN_REFRESH, policy(
                limit(RateLimitKeyType.IP_AND_TOKEN, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_GUEST_SESSION, policy(
                limit(RateLimitKeyType.IP_AND_INSTALLATION, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_PROJECT_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_PROJECT_COMPARE, policy(
                limit(RateLimitKeyType.IP_OR_USER, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_LOCATION_RESOLVE, policy(
                limit(RateLimitKeyType.IP_AND_DEVICE, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CITY_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_BUILDER_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_BUSINESS_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_PROVIDER_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_APP_CONTENT_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CALCULATOR_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));

        ObjectMapper objectMapper = new ObjectMapper();
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);

        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitPolicyResolver(),
                new RateLimitKeyResolver(),
                new ClientIpResolver(properties),
                new InMemoryRateLimitService(properties),
                properties,
                objectMapper,
                jwtTokenUtil
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyRoutes())
                .addFilters(filter)
                .build();

        Logger logger = (Logger) LoggerFactory.getLogger(RateLimitingFilter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(RateLimitingFilter.class);
        logger.detachAppender(logAppender);
    }

    private String json(Map<String, Object> fields) throws Exception {
        return new ObjectMapper().writeValueAsString(fields);
    }

    @Test
    void requestOtpExceedingPhoneLimitReturns429() throws Exception {
        String body = json(Map.of("phoneNumber", "9876543210"));

        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("1.1.1.1")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("1.1.1.1")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("1.1.1.1")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("MOBILE_OTP_REQUEST"));
    }

    @Test
    void differentPhoneNumbersGetIndependentBuckets() throws Exception {
        String phoneA = json(Map.of("phoneNumber", "9876543210"));
        String phoneB = json(Map.of("phoneNumber", "9876543211"));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(phoneA)
                            .with(remoteAddr("1.1.1.1")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(phoneA)
                        .with(remoteAddr("1.1.1.1")))
                .andExpect(status().isTooManyRequests());

        // A fresh phone number, same IP, must not be blocked by phoneA's exhausted bucket.
        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(phoneB)
                        .with(remoteAddr("1.1.1.1")))
                .andExpect(status().isOk());
    }

    @Test
    void requestOtpSameIpAcrossManyPhonesEventuallyHitsIpLimit() throws Exception {
        // IP limit is 3/hour; each distinct phone number consumes 1 IP token too.
        for (int i = 0; i < 3; i++) {
            String body = json(Map.of("phoneNumber", "987654321" + i));
            mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                            .with(remoteAddr("2.2.2.2")))
                    .andExpect(status().isOk());
        }

        String fourthPhone = json(Map.of("phoneNumber", "9876543219"));
        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(fourthPhone)
                        .with(remoteAddr("2.2.2.2")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void verifyOtpExceedingLimitReturns429() throws Exception {
        String body = json(Map.of("phoneNumber", "9876543210", "code", "111111"));

        mockMvc.perform(post("/api/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/verify-otp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void refreshTokenNeverAppearsInLogsEvenWhenBlocked() throws Exception {
        String rawToken = "super-secret-raw-refresh-token-value";
        String body = json(Map.of("refreshToken", rawToken));

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("3.3.3.3")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("3.3.3.3")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("3.3.3.3")))
                .andExpect(status().isTooManyRequests());

        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());

        assertThat(logMessages).isNotEmpty();
        assertThat(logMessages).noneMatch(message -> message.contains(rawToken));
    }

    @Test
    void guestSessionExceedingInstallationAndIpLimitReturns429() throws Exception {
        String body = json(Map.of("installationId", "install-abc"));

        mockMvc.perform(post("/api/auth/guest/session").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("4.4.4.4")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/guest/session").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("4.4.4.4")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/guest/session").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("4.4.4.4")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void homeReadAllowsUnderLimitAndBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/home").with(remoteAddr("5.5.5.5"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/home").with(remoteAddr("5.5.5.5"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/home").with(remoteAddr("5.5.5.5"))).andExpect(status().isTooManyRequests());
    }

    @Test
    void projectReadAllowsUnderLimitAndBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/projects/1").with(remoteAddr("6.6.6.6"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/projects/2").with(remoteAddr("6.6.6.6"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/3").with(remoteAddr("6.6.6.6"))).andExpect(status().isTooManyRequests());
    }

    @Test
    void projectCompareBlocksAfterLimit() throws Exception {
        String body = json(Map.of("projectIds", List.of(1, 2)));

        mockMvc.perform(post("/api/projects/compare").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("7.7.7.7")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/projects/compare").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("7.7.7.7")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/compare").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("7.7.7.7")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void locationResolveBlocksAfterLimit() throws Exception {
        String body = json(Map.of("latitude", 12.9, "longitude", 77.6));

        mockMvc.perform(post("/api/location/resolve").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("8.8.8.8")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/location/resolve").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("8.8.8.8")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/location/resolve").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("8.8.8.8")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void retryAfterHeaderIsPresentOnBlockedResponse() throws Exception {
        mockMvc.perform(get("/api/home").with(remoteAddr("9.9.9.9"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/home").with(remoteAddr("9.9.9.9"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/home").with(remoteAddr("9.9.9.9")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    // ── Phase 1.5: remaining public mobile/public read APIs ─────────────────────

    @Test
    void publicCityReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/cities").with(remoteAddr("15.15.15.15"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/cities").with(remoteAddr("15.15.15.15"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/cities").with(remoteAddr("15.15.15.15")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_CITY_READ"));
    }

    @Test
    void postCitiesIsNeverRateLimitedByPublicCityReadPolicy() throws Exception {
        // Distinct from the GET policy entirely - POST has no mapped policy in
        // Phase 1/1.5, so it must never be blocked regardless of GET traffic volume.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/cities").with(remoteAddr("15.15.15.15"))).andExpect(status().isOk());
        }
    }

    @Test
    void publicBuilderReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/builders/1").with(remoteAddr("16.16.16.16"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/builders/2").with(remoteAddr("16.16.16.16"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/builders/3").with(remoteAddr("16.16.16.16")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicBusinessReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/businesses/1").with(remoteAddr("17.17.17.17"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/businesses/2").with(remoteAddr("17.17.17.17"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/businesses/3").with(remoteAddr("17.17.17.17")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicProviderReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/providers/1").with(remoteAddr("18.18.18.18"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/providers/2").with(remoteAddr("18.18.18.18"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/providers/3").with(remoteAddr("18.18.18.18")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicAppContentReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/app-content/pages/about-us").with(remoteAddr("19.19.19.19")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/app/screen-content").with(remoteAddr("19.19.19.19")))
                .andExpect(status().isOk());

        // Both routes share the same PUBLIC_APP_CONTENT_READ policy/IP bucket.
        mockMvc.perform(get("/api/app-content/pages/contact-us").with(remoteAddr("19.19.19.19")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void postAppScreenContentIsNeverRateLimitedByAppContentReadPolicy() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/app/screen-content").with(remoteAddr("19.19.19.19")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void publicCalculatorReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/public/stamp-duty/cities").with(remoteAddr("20.20.20.20")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/stamp-duty/cities").with(remoteAddr("20.20.20.20")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/stamp-duty/cities").with(remoteAddr("20.20.20.20")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicBuilderReadRequestsAreNeverBodyWrapped() throws Exception {
        // Same guarantee as PUBLIC_HOME_READ: GET routes for the new Phase 1.5
        // policies must never go through the size-checked body-caching path.
        MockMvc tinyLimitMockMvc = buildMockMvcWithMaxBodyBytesForBuilderRead(1);
        String oversizedContent = "x".repeat(10_000);

        tinyLimitMockMvc.perform(get("/api/builders/1").content(oversizedContent).with(remoteAddr("21.21.21.21")))
                .andExpect(status().isOk());
    }

    private MockMvc buildMockMvcWithMaxBodyBytesForBuilderRead(long maxBodyBytes) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxCachedBodyBytes(maxBodyBytes);
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_BUILDER_READ, policy(
                limit(RateLimitKeyType.IP, 100, 60)
        ));

        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitPolicyResolver(),
                new RateLimitKeyResolver(),
                new ClientIpResolver(properties),
                new InMemoryRateLimitService(properties),
                properties,
                new ObjectMapper(),
                mock(JwtTokenUtil.class)
        );

        return MockMvcBuilders.standaloneSetup(new DummyRoutes())
                .addFilters(filter)
                .build();
    }

    // ── Check 2: cached body size protection ────────────────────────────────────

    @Test
    void smallOtpRequestBodyWorksNormally() throws Exception {
        String body = json(Map.of("phoneNumber", "9876543210"));

        mockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("10.10.10.10")))
                .andExpect(status().isOk());
    }

    @Test
    void oversizedRequestBodyReturns413BeforeControllerRuns() throws Exception {
        MockMvc tinyLimitMockMvc = buildMockMvcWithMaxBodyBytes(10);
        String body = json(Map.of("phoneNumber", "9876543210")); // well over 10 bytes

        tinyLimitMockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("11.11.11.11")))
                .andExpect(status().isContentTooLarge())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value(413));
    }

    @Test
    void getPublicReadRequestsAreNeverBodyWrapped() throws Exception {
        // GET requests to a body-unaware policy must never go through the
        // size-checked caching path at all, even with a body attached and even
        // with a max-body-bytes limit so small it would reject any POST body.
        MockMvc tinyLimitMockMvc = buildMockMvcWithMaxBodyBytes(1);
        String oversizedContent = "x".repeat(10_000);

        tinyLimitMockMvc.perform(get("/api/home").content(oversizedContent).with(remoteAddr("13.13.13.13")))
                .andExpect(status().isOk());
    }

    @Test
    void rawRequestBodyIsNeverLoggedWhenBodyIsRejectedAsTooLarge() throws Exception {
        MockMvc tinyLimitMockMvc = buildMockMvcWithMaxBodyBytes(10);
        String marker = "unique-body-marker-should-never-appear-in-logs";
        String body = json(Map.of("phoneNumber", marker));

        tinyLimitMockMvc.perform(post("/api/auth/request-otp").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("14.14.14.14")))
                .andExpect(status().isContentTooLarge());

        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());

        assertThat(logMessages).isNotEmpty();
        assertThat(logMessages).noneMatch(message -> message.contains(marker));
    }

    private MockMvc buildMockMvcWithMaxBodyBytes(long maxBodyBytes) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxCachedBodyBytes(maxBodyBytes);
        properties.getPolicies().put(RateLimitPolicy.MOBILE_OTP_REQUEST, policy(
                limit(RateLimitKeyType.PHONE, 100, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy(
                limit(RateLimitKeyType.IP, 100, 60)
        ));

        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitPolicyResolver(),
                new RateLimitKeyResolver(),
                new ClientIpResolver(properties),
                new InMemoryRateLimitService(properties),
                properties,
                new ObjectMapper(),
                mock(JwtTokenUtil.class)
        );

        return MockMvcBuilders.standaloneSetup(new DummyRoutes())
                .addFilters(filter)
                .build();
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
