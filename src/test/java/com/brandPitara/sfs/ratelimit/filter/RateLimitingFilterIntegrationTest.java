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
import static org.mockito.Mockito.when;
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

        // Phase 2 dummy routes.
        @PostMapping("/api/public/stamp-duty/calculate")
        String stampDutyCalculate() {
            return "ok";
        }

        @GetMapping("/api/profile")
        String myProfile() {
            return "ok";
        }

        @PostMapping("/api/project-favorites/{id}/toggle")
        String toggleFavorite() {
            return "ok";
        }

        @PostMapping("/api/projects/{id}/reviews")
        String submitReview() {
            return "ok";
        }

        @GetMapping("/api/dashboard/something")
        String dashboardSomething() {
            return "ok";
        }

        // Phase 3 dummy routes.
        @PostMapping("/api/customer/requests")
        String createCustomerRequest() {
            return "ok";
        }

        @PostMapping("/api/providers/me/requests/{id}/interest")
        String providerInterest() {
            return "ok";
        }

        @PostMapping("/api/onboarding/choose-role")
        String chooseRole() {
            return "ok";
        }

        @PostMapping("/api/businesses/{id}/events")
        String businessEvent() {
            return "ok";
        }

        @GetMapping("/api/public/companies")
        String publicCompanies() {
            return "ok";
        }

        @GetMapping("/api/public/architect-designers/{id}")
        String publicArchitectDesigner() {
            return "ok";
        }

        @GetMapping("/api/public/instagram-reels")
        String publicInstagramReels() {
            return "ok";
        }

        @GetMapping("/api/public/project-meter/cards")
        String publicProjectMeterCards() {
            return "ok";
        }

        @GetMapping("/api/public/feed")
        String publicFeed() {
            return "ok";
        }

        // Phase 4 dummy routes.
        @GetMapping("/api/brands")
        String brands() {
            return "ok";
        }

        @GetMapping("/api/brands/{id}/distributors")
        String brandDistributors() {
            return "ok";
        }

        @GetMapping("/api/distributors/{id}")
        String distributorDetail() {
            return "ok";
        }

        @GetMapping("/api/categories")
        String categories() {
            return "ok";
        }

        @GetMapping("/api/public/cities/trending")
        String trendingCities() {
            return "ok";
        }

        @GetMapping("/api/content/version")
        String contentVersion() {
            return "ok";
        }

        @GetMapping("/api/session/me")
        String sessionMe() {
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

        // Phase 2: mobile action APIs.
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE, policy(
                limit(RateLimitKeyType.IP, 100, 60),
                limit(RateLimitKeyType.BODY_FINGERPRINT, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_PROFILE_READ, policy(
                limit(RateLimitKeyType.IP_OR_USER, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_FAVORITE_WRITE, policy(
                limit(RateLimitKeyType.IP_OR_USER, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_REVIEW_WRITE, policy(
                limit(RateLimitKeyType.IP_OR_USER, 2, 60)
        ));

        // Phase 3: remaining mobile/public API rate limiting.
        properties.getPolicies().put(RateLimitPolicy.MOBILE_SERVICE_REQUEST_WRITE, policy(
                limit(RateLimitKeyType.IP_OR_USER, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_PROVIDER_INTEREST_WRITE, policy(
                limit(RateLimitKeyType.IP_OR_USER, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_ONBOARDING_WRITE, policy(
                limit(RateLimitKeyType.IP_OR_USER, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_BUSINESS_EVENT_WRITE, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_COMPANY_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_ARCHITECT_DESIGNER_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_INSTAGRAM_REELS_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_PROJECT_METER_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_FEED_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));

        // Phase 4: final remaining mobile/public read route policies.
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_BRAND_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_DISTRIBUTOR_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CATEGORY_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        // PUBLIC_CITY_READ is already registered above (Phase 1.5) - reused as-is
        // for GET /api/public/cities/trending, no duplicate config needed.
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CONTENT_VERSION_READ, policy(
                limit(RateLimitKeyType.IP, 2, 60)
        ));
        properties.getPolicies().put(RateLimitPolicy.MOBILE_SESSION_READ, policy(
                limit(RateLimitKeyType.IP_OR_USER, 2, 60)
        ));

        ObjectMapper objectMapper = new ObjectMapper();
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        when(jwtTokenUtil.getUserIdFromToken(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(42L);

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

    private static org.springframework.test.web.servlet.request.RequestPostProcessor bearerToken(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    // ── Phase 2: mobile action APIs ──────────────────────────────────────────────

    @Test
    void publicCalculatorPostReturns429AfterConfiguredTestLimit() throws Exception {
        String bodyA = json(Map.of("cityId", 1, "propertyValue", 5_000_000));

        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(bodyA)
                        .with(remoteAddr("30.30.30.30")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(bodyA)
                        .with(remoteAddr("30.30.30.30")))
                .andExpect(status().isOk());

        // BODY_FINGERPRINT limit is 2/min for identical bodies; IP limit is 100/min
        // so this third request is blocked specifically on the fingerprint dimension.
        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(bodyA)
                        .with(remoteAddr("30.30.30.30")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_CALCULATOR_WRITE"));
    }

    @Test
    void differentCalculatorBodiesGetIndependentFingerprintBuckets() throws Exception {
        String bodyA = json(Map.of("cityId", 1, "propertyValue", 5_000_000));
        String bodyB = json(Map.of("cityId", 2, "propertyValue", 5_000_000));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(bodyA)
                            .with(remoteAddr("31.31.31.31")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(bodyA)
                        .with(remoteAddr("31.31.31.31")))
                .andExpect(status().isTooManyRequests());

        // A different body, same IP, must not be blocked by bodyA's exhausted fingerprint bucket.
        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(bodyB)
                        .with(remoteAddr("31.31.31.31")))
                .andExpect(status().isOk());
    }

    @Test
    void oversizedCalculatorPostBodyReturns413BeforeControllerRuns() throws Exception {
        MockMvc tinyLimitMockMvc = buildMockMvcWithMaxBodyBytesForCalculatorWrite(10);
        String body = json(Map.of("cityId", 1, "propertyValue", 5_000_000)); // well over 10 bytes

        tinyLimitMockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("32.32.32.32")))
                .andExpect(status().isContentTooLarge())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value(413));
    }

    @Test
    void rawCalculatorBodyIsNeverLoggedWhenBlockedOrRejected() throws Exception {
        String marker = "unique-calculator-body-marker-should-never-appear-in-logs";
        String body = json(Map.of("cityId", 1, "note", marker));

        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("33.33.33.33")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("33.33.33.33")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/stamp-duty/calculate").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("33.33.33.33")))
                .andExpect(status().isTooManyRequests());

        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());

        assertThat(logMessages).isNotEmpty();
        assertThat(logMessages).noneMatch(message -> message.contains(marker));
    }

    private MockMvc buildMockMvcWithMaxBodyBytesForCalculatorWrite(long maxBodyBytes) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxCachedBodyBytes(maxBodyBytes);
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CALCULATOR_WRITE, policy(
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

    @Test
    void authenticatedMobileProfileRouteReturns429AfterLimit() throws Exception {
        mockMvc.perform(get("/api/profile").with(bearerToken("token-a")).with(remoteAddr("34.34.34.34")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/profile").with(bearerToken("token-a")).with(remoteAddr("34.34.34.34")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profile").with(bearerToken("token-a")).with(remoteAddr("34.34.34.34")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("MOBILE_PROFILE_READ"));
    }

    @Test
    void authenticatedFavoritesWriteReturns429AfterLimit() throws Exception {
        mockMvc.perform(post("/api/project-favorites/1/toggle").with(bearerToken("token-a")).with(remoteAddr("35.35.35.35")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/project-favorites/2/toggle").with(bearerToken("token-a")).with(remoteAddr("35.35.35.35")))
                .andExpect(status().isOk());

        // Same authenticated user (userId 42 from the mocked JWT), different project id -
        // still the same per-user bucket, so the third toggle is blocked.
        mockMvc.perform(post("/api/project-favorites/3/toggle").with(bearerToken("token-a")).with(remoteAddr("35.35.35.35")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("MOBILE_FAVORITE_WRITE"));
    }

    @Test
    void authenticatedReviewWriteReturns429AfterLimit() throws Exception {
        String body = json(Map.of("rating", 5, "comment", "great"));

        mockMvc.perform(post("/api/projects/1/reviews").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("36.36.36.36")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/projects/2/reviews").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("36.36.36.36")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/3/reviews").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("36.36.36.36")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("MOBILE_REVIEW_WRITE"));
    }

    @Test
    void mobileProfileGetRequestsAreNeverBodyWrapped() throws Exception {
        // GET routes must never go through the size-checked body-caching path,
        // even with a max-body-bytes limit so small it would reject any POST body.
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxCachedBodyBytes(1);
        properties.getPolicies().put(RateLimitPolicy.MOBILE_PROFILE_READ, policy(
                limit(RateLimitKeyType.IP_OR_USER, 100, 60)
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
        MockMvc tinyLimitMockMvc = MockMvcBuilders.standaloneSetup(new DummyRoutes()).addFilters(filter).build();

        String oversizedContent = "x".repeat(10_000);
        tinyLimitMockMvc.perform(get("/api/profile").content(oversizedContent).with(remoteAddr("37.37.37.37")))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardRouteIsUntouchedRegardlessOfVolume() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/dashboard/something").with(remoteAddr("38.38.38.38")))
                    .andExpect(status().isOk());
        }
    }

    // ── Phase 3: remaining mobile/public API rate limiting ──────────────────────

    @Test
    void mobileServiceRequestWriteReturns429AfterLimitUsingUserIdKey() throws Exception {
        String body = json(Map.of("title", "need a plumber"));

        mockMvc.perform(post("/api/customer/requests").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("40.40.40.40")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/customer/requests").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("40.40.40.40")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/customer/requests").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("40.40.40.40")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("MOBILE_SERVICE_REQUEST_WRITE"));
    }

    @Test
    void mobileProviderInterestWriteReturns429AfterLimitUsingUserIdKey() throws Exception {
        mockMvc.perform(post("/api/providers/me/requests/1/interest")
                        .with(bearerToken("token-a")).with(remoteAddr("41.41.41.41")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/providers/me/requests/2/interest")
                        .with(bearerToken("token-a")).with(remoteAddr("41.41.41.41")))
                .andExpect(status().isOk());

        // Same authenticated user, different requestId path variable - still the
        // same per-user bucket (path-variable keying was intentionally not added).
        mockMvc.perform(post("/api/providers/me/requests/3/interest")
                        .with(bearerToken("token-a")).with(remoteAddr("41.41.41.41")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("MOBILE_PROVIDER_INTEREST_WRITE"));
    }

    @Test
    void mobileOnboardingWriteReturns429AfterLimitAndNeverLogsRawBody() throws Exception {
        String marker = "unique-onboarding-body-marker-should-never-appear-in-logs";
        String body = json(Map.of("role", "CUSTOMER", "note", marker));

        mockMvc.perform(post("/api/onboarding/choose-role").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("42.42.42.42")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/onboarding/choose-role").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("42.42.42.42")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/onboarding/choose-role").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(bearerToken("token-a")).with(remoteAddr("42.42.42.42")))
                .andExpect(status().isTooManyRequests());

        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());

        assertThat(logMessages).isNotEmpty();
        assertThat(logMessages).noneMatch(message -> message.contains(marker));
    }

    @Test
    void publicBusinessEventWriteReturns429AfterLimitUsingIpKey() throws Exception {
        String body = json(Map.of("eventType", "VIEW"));

        mockMvc.perform(post("/api/businesses/1/events").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("43.43.43.43")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/businesses/2/events").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("43.43.43.43")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/businesses/3/events").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(remoteAddr("43.43.43.43")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_BUSINESS_EVENT_WRITE"));
    }

    @Test
    void publicBusinessEventWriteIsNotBodyWrapped() throws Exception {
        // PUBLIC_BUSINESS_EVENT_WRITE keys on IP alone - it must never buffer the
        // request body, even with a max-body-bytes limit so small it would reject
        // any POST body.
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxCachedBodyBytes(1);
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_BUSINESS_EVENT_WRITE, policy(
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
        MockMvc tinyLimitMockMvc = MockMvcBuilders.standaloneSetup(new DummyRoutes()).addFilters(filter).build();

        String oversizedContent = "x".repeat(10_000);
        tinyLimitMockMvc.perform(post("/api/businesses/1/events").content(oversizedContent).with(remoteAddr("44.44.44.44")))
                .andExpect(status().isOk());
    }

    @Test
    void publicCompanyReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/public/companies").with(remoteAddr("45.45.45.45"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/companies").with(remoteAddr("45.45.45.45"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/companies").with(remoteAddr("45.45.45.45")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_COMPANY_READ"));
    }

    @Test
    void publicArchitectDesignerReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/public/architect-designers/1").with(remoteAddr("46.46.46.46"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/architect-designers/2").with(remoteAddr("46.46.46.46"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/architect-designers/3").with(remoteAddr("46.46.46.46")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicInstagramReelsReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/public/instagram-reels").with(remoteAddr("47.47.47.47"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/instagram-reels").with(remoteAddr("47.47.47.47"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/instagram-reels").with(remoteAddr("47.47.47.47")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicProjectMeterReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/public/project-meter/cards").with(remoteAddr("48.48.48.48"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/project-meter/cards").with(remoteAddr("48.48.48.48"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/project-meter/cards").with(remoteAddr("48.48.48.48")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicFeedReadBlocksAfterLimit() throws Exception {
        mockMvc.perform(get("/api/public/feed").with(remoteAddr("49.49.49.49"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/feed").with(remoteAddr("49.49.49.49"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/feed").with(remoteAddr("49.49.49.49")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void publicFeedReadRequestsAreNeverBodyWrapped() throws Exception {
        MockMvc tinyLimitMockMvc = buildMockMvcWithMaxBodyBytesForFeedRead(1);
        String oversizedContent = "x".repeat(10_000);

        tinyLimitMockMvc.perform(get("/api/public/feed").content(oversizedContent).with(remoteAddr("50.50.50.50")))
                .andExpect(status().isOk());
    }

    private MockMvc buildMockMvcWithMaxBodyBytesForFeedRead(long maxBodyBytes) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxCachedBodyBytes(maxBodyBytes);
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_FEED_READ, policy(
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

    @Test
    void dashboardAndAdminRoutesRemainUntouchedByPhase3Policies() throws Exception {
        // /api/dashboard/** and /api/admin/** are handled by a separate security
        // filter chain in production and never even reach RateLimitingFilter; this
        // proves the resolver itself also never maps them, as defense in depth.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/dashboard/something").with(remoteAddr("51.51.51.51")))
                    .andExpect(status().isOk());
        }
    }

    // ── Phase 4: final remaining mobile/public read route policies ──────────────

    @Test
    void representativeBrandReadBlocksAfterConfiguredTestLimit() throws Exception {
        mockMvc.perform(get("/api/brands").with(remoteAddr("52.52.52.52"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/brands").with(remoteAddr("52.52.52.52"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/brands").with(remoteAddr("52.52.52.52")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_BRAND_READ"));
    }

    @Test
    void representativeDistributorReadBlocksAfterConfiguredTestLimit() throws Exception {
        mockMvc.perform(get("/api/distributors/1").with(remoteAddr("53.53.53.53"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/distributors/2").with(remoteAddr("53.53.53.53"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/distributors/3").with(remoteAddr("53.53.53.53")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_DISTRIBUTOR_READ"));
    }

    @Test
    void brandDistributorsRouteUsesDistributorPolicyNotBrandPolicy() throws Exception {
        mockMvc.perform(get("/api/brands/1/distributors").with(remoteAddr("54.54.54.54"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/brands/2/distributors").with(remoteAddr("54.54.54.54"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/brands/3/distributors").with(remoteAddr("54.54.54.54")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_DISTRIBUTOR_READ"));
    }

    @Test
    void representativeCategoryReadBlocksAfterConfiguredTestLimit() throws Exception {
        mockMvc.perform(get("/api/categories").with(remoteAddr("55.55.55.55"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/categories").with(remoteAddr("55.55.55.55"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/categories").with(remoteAddr("55.55.55.55")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_CATEGORY_READ"));
    }

    @Test
    void trendingCityRouteBlocksAfterConfiguredTestLimitUsingReusedPolicy() throws Exception {
        mockMvc.perform(get("/api/public/cities/trending").with(remoteAddr("56.56.56.56"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/cities/trending").with(remoteAddr("56.56.56.56"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/cities/trending").with(remoteAddr("56.56.56.56")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_CITY_READ"));
    }

    @Test
    void contentVersionRouteUsesItsOwnGenerousReadPolicy() throws Exception {
        // Configured test limit for PUBLIC_CONTENT_VERSION_READ is the same tiny
        // 2/60s as other policies here (production config is 600/min) - this test
        // only proves the route is governed by its own dedicated policy, not that
        // production traffic would ever realistically hit it.
        mockMvc.perform(get("/api/content/version").with(remoteAddr("57.57.57.57"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/content/version").with(remoteAddr("57.57.57.57"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/content/version").with(remoteAddr("57.57.57.57")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("PUBLIC_CONTENT_VERSION_READ"));
    }

    @Test
    void sessionMeRouteUsesAuthenticatedUserIdWhenAvailable() throws Exception {
        mockMvc.perform(get("/api/session/me").with(bearerToken("token-a")).with(remoteAddr("58.58.58.58")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/session/me").with(bearerToken("token-a")).with(remoteAddr("58.58.58.58")))
                .andExpect(status().isOk());

        // Same authenticated user (userId 42 from the mocked JWT) is blocked on the
        // third call even though nothing else about the request changed.
        mockMvc.perform(get("/api/session/me").with(bearerToken("token-a")).with(remoteAddr("58.58.58.58")))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.policy")
                        .value("MOBILE_SESSION_READ"));
    }

    @Test
    void sessionMeRouteFallsBackToIpWhenUnauthenticated() throws Exception {
        // No Authorization header - currentUserId() resolves to null, so the
        // IP_OR_USER dimension falls back to IP instead of failing the request.
        mockMvc.perform(get("/api/session/me").with(remoteAddr("59.59.59.59"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/session/me").with(remoteAddr("59.59.59.59"))).andExpect(status().isOk());

        mockMvc.perform(get("/api/session/me").with(remoteAddr("59.59.59.59")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void phase4GetRoutesAreNeverBodyWrapped() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setMaxCachedBodyBytes(1);
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_CATEGORY_READ, policy(
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
        MockMvc tinyLimitMockMvc = MockMvcBuilders.standaloneSetup(new DummyRoutes()).addFilters(filter).build();

        String oversizedContent = "x".repeat(10_000);
        tinyLimitMockMvc.perform(get("/api/categories").content(oversizedContent).with(remoteAddr("60.60.60.60")))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardRouteRemainsUntouchedByPhase4Policies() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/dashboard/something").with(remoteAddr("61.61.61.61")))
                    .andExpect(status().isOk());
        }
    }
}
