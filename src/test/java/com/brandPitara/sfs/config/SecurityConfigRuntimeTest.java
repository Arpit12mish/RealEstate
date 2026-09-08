package com.brandPitara.sfs.config;

import com.brandPitara.sfs.dashboard.auth.security.DashboardAccessDeniedHandler;
import com.brandPitara.sfs.dashboard.auth.security.DashboardAuthenticationEntryPoint;
import com.brandPitara.sfs.dashboard.auth.security.DashboardJwtAuthenticationFilter;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetailsService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardJwtService;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.filter.RateLimitingFilter;
import com.brandPitara.sfs.ratelimit.filter.PreAuthenticationAbuseFilter;
import com.brandPitara.sfs.ratelimit.metrics.RateLimitMetrics;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitKeyResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import com.brandPitara.sfs.ratelimit.service.RateLimitService;
import com.brandPitara.sfs.ratelimit.service.impl.InMemoryRateLimitService;
import com.brandPitara.sfs.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigRuntimeTest {

    private MockMvc mockMvc;
    private AnnotationConfigWebApplicationContext context;
    private FilterChainProxy securityFilterChain;

    @BeforeEach
    void setUp() {
        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        webContext.setServletContext(new MockServletContext());
        webContext.register(TestMvcConfig.class);
        webContext.refresh();

        Filter springSecurityFilterChain = webContext.getBean("springSecurityFilterChain", Filter.class);
        securityFilterChain = (FilterChainProxy) springSecurityFilterChain;
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext)
                .addFilters(springSecurityFilterChain)
                .build();
        context = webContext;
    }

    @Test
    void filtersRunPreAuthThenJwtThenPrincipalLimiterOnTheAppChain() {
        var appFilters = securityFilterChain.getFilterChains().stream()
                .map(chain -> chain.getFilters())
                .filter(filters -> filters.stream().anyMatch(JwtRequestFilter.class::isInstance))
                .findFirst()
                .orElseThrow();
        int jwtIndex = indexOf(appFilters, JwtRequestFilter.class);
        int preAuthIndex = indexOf(appFilters, PreAuthenticationAbuseFilter.class);
        int rateIndex = indexOf(appFilters, RateLimitingFilter.class);

        assertThat(preAuthIndex).isGreaterThanOrEqualTo(0);
        assertThat(jwtIndex).isGreaterThan(preAuthIndex);
        assertThat(jwtIndex).isGreaterThanOrEqualTo(0);
        assertThat(rateIndex).isGreaterThan(jwtIndex);
    }

    private int indexOf(java.util.List<Filter> filters, Class<?> type) {
        for (int index = 0; index < filters.size(); index++) {
            if (type.isInstance(filters.get(index))) return index;
        }
        return -1;
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void configuredMobileAuthPostEndpointsArePublicAtRuntime() throws Exception {
        for (String endpoint : SecurityConfig.PUBLIC_MOBILE_AUTH_ENDPOINTS) {
            mockMvc.perform(post(endpoint))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void unknownAuthEndpointsAreProtectedAtRuntime() throws Exception {
        mockMvc.perform(get("/api/auth/random"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/random"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardLoginRemainsPublicAtRuntime() throws Exception {
        mockMvc.perform(post("/api/dashboard/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCannotUpdatePromoBanner() throws Exception {
        mockMvc.perform(put("/api/dashboard/promo-banners/99"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUserCannotManageDashboardUsers() throws Exception {
        mockMvc.perform(get("/api/dashboard/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUserCannotReadOrUpdateCmsDocument() throws Exception {
        mockMvc.perform(get("/api/dashboard/cms/content/9/document"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/dashboard/cms/content/9/document"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/dashboard/cms/media/uploads"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/dashboard/cms/media/9/complete"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/cms/media"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/cms/media/9"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/dashboard/cms/content/9/workflow/submit"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/dashboard/cms/content/9/workflow/approve"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/dashboard/cms/content/9/workflow/publish"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/cms/content/9/revisions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/cms/content/9/workflow-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void appScreenContentIsPublicAtRuntime() throws Exception {
        mockMvc.perform(get("/api/app/screen-content"))
                .andExpect(status().isOk());
    }

    @Test
    void publicCmsContentIsUnauthenticatedAtRuntime() throws Exception {
        mockMvc.perform(get("/api/public/content"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/content/published-slug"))
                .andExpect(status().isOk());
    }

    @Test
    void projectsCompareIsPublicAtRuntime() throws Exception {
        mockMvc.perform(post("/api/projects/compare"))
                .andExpect(status().isOk());
    }

    @Test
    void publicCalculatorWriteEndpointsAreUnauthenticatedAtRuntime() throws Exception {
        mockMvc.perform(post("/api/public/interior-cost/compare"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/interior-cost/compare-custom"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/circle-rates/calculate"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/stamp-duty/calculate"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCalculatorWriteEndpointsRemainProtectedAtRuntime() throws Exception {
        // The new public POST matchers are scoped to /api/public/**; the admin
        // calculator CRUD surface lives under /api/admin/** on the separate
        // dashboardFilterChain and must stay authenticated.
        mockMvc.perform(post("/api/admin/interior-cost/rules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unrelatedAppAndProjectEndpointsRemainProtectedAtRuntime() throws Exception {
        mockMvc.perform(get("/api/app/other-content"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects/other"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicCityDetailEndpointIsPublicAtRuntimeWithoutAuthorization() throws Exception {
        // GAP-017: GET /api/public/cities/{citySlug}. Covered by the pre-existing
        // "GET /api/public/**" permitAll matcher - no SecurityConfig change was
        // made for this endpoint. This test proves that matcher genuinely covers
        // a nested /api/public/cities/* path at runtime, not just /api/public/cities/trending.
        mockMvc.perform(get("/api/public/cities/mumbai"))
                .andExpect(status().isOk());
    }

    @Test
    void mobileUpdatePolicyIsPublicButDashboardPolicyUpdatesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/public/mobile-app/update-policy")
                        .param("platform", "ANDROID")
                        .param("currentBuild", "20"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/dashboard/mobile-app/update-policies/ANDROID"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/dashboard/mobile-app/update-policies/ANDROID/emergency-disable"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deterministicPublicV2ReturnsSameBodyWithNoTokenAndMalformedAuthorization() throws Exception {
        String anonymous = mockMvc.perform(get("/api/v2/public/projects/27"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String malformed = mockMvc.perform(get("/api/v2/public/projects/27")
                        .header("Authorization", "malformed"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(malformed).isEqualTo(anonymous);
    }

    @Test
    void deterministicPublicV2HeadIsPublicEvenWithMalformedAuthorization() throws Exception {
        mockMvc.perform(head("/api/v2/public/projects/27")
                        .header("Authorization", "malformed"))
                .andExpect(status().isOk());
    }

    @Test
    void favoriteOverlayRequiresAuthenticationAtRuntime() throws Exception {
        mockMvc.perform(get("/api/me/project-favorites").param("projectIds", "27"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorReadinessProbeIsPublicButMetricsRemainProtected() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics/hikaricp.connections.active"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class ActuatorSecurityTestController {

        @GetMapping("/actuator/health/readiness")
        ResponseEntity<Void> readiness() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/actuator/metrics/hikaricp.connections.active")
        ResponseEntity<Void> hikariMetric() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/auth")
    static class MobileAuthTestController {

        @PostMapping("/request-otp")
        ResponseEntity<Void> requestOtp() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/otp/resend")
        ResponseEntity<Void> resendOtp() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/verify-otp")
        ResponseEntity<Void> verifyOtp() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/refresh")
        ResponseEntity<Void> refresh() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/logout")
        ResponseEntity<Void> logout() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/logout-all")
        ResponseEntity<Void> logoutAll() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/guest/session")
        ResponseEntity<Void> guestSession() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/dashboard/auth")
    static class DashboardAuthTestController {

        @PostMapping("/login")
        ResponseEntity<Void> login() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/dashboard/promo-banners")
    static class DashboardPromoBannerTestController {

        @PutMapping("/{id}")
        ResponseEntity<Void> update() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/app")
    static class AppContentTestController {

        @GetMapping("/screen-content")
        ResponseEntity<Void> screenContent() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/other-content")
        ResponseEntity<Void> otherContent() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/public/cities")
    static class PublicCitiesTestController {

        @GetMapping("/{citySlug}")
        ResponseEntity<Void> getBySlug() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    static class MobileUpdatePolicySecurityTestController {
        @GetMapping("/api/public/mobile-app/update-policy")
        ResponseEntity<Void> publicPolicy() {
            return ResponseEntity.ok().build();
        }

        @PutMapping("/api/dashboard/mobile-app/update-policies/{platform}")
        ResponseEntity<Void> updatePolicy() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/v2/public/projects")
    static class PublicV2ProjectsTestController {

        @GetMapping("/{projectId}")
        ResponseEntity<String> get() {
            return ResponseEntity.ok("deterministic");
        }
    }

    @RestController
    @RequestMapping("/api/public/content")
    static class PublicCmsContentTestController {
        @GetMapping
        ResponseEntity<Void> list() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/{slug}")
        ResponseEntity<Void> detail() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/me/project-favorites")
    static class FavoriteOverlayTestController {

        @GetMapping
        ResponseEntity<Void> get() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/projects")
    static class ProjectsTestController {

        @PostMapping("/compare")
        ResponseEntity<Void> compare() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/other")
        ResponseEntity<Void> other() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/public/interior-cost")
    static class PublicInteriorCostTestController {

        @PostMapping("/compare")
        ResponseEntity<Void> compare() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/compare-custom")
        ResponseEntity<Void> compareCustom() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/public/circle-rates")
    static class PublicCircleRateTestController {

        @PostMapping("/calculate")
        ResponseEntity<Void> calculate() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/public/stamp-duty")
    static class PublicStampDutyTestController {

        @PostMapping("/calculate")
        ResponseEntity<Void> calculate() {
            return ResponseEntity.ok().build();
        }
    }

    @RestController
    @RequestMapping("/api/admin/interior-cost")
    static class AdminInteriorCostTestController {

        @PostMapping("/rules")
        ResponseEntity<Void> createRule() {
            return ResponseEntity.ok().build();
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class TestMvcConfig {

        @Bean
        MobileAuthTestController mobileAuthTestController() {
            return new MobileAuthTestController();
        }

        @Bean
        DashboardAuthTestController dashboardAuthTestController() {
            return new DashboardAuthTestController();
        }

        @Bean
        DashboardPromoBannerTestController dashboardPromoBannerTestController() {
            return new DashboardPromoBannerTestController();
        }

        @Bean
        AppContentTestController appContentTestController() {
            return new AppContentTestController();
        }

        @Bean
        ProjectsTestController projectsTestController() {
            return new ProjectsTestController();
        }

        @Bean
        PublicInteriorCostTestController publicInteriorCostTestController() {
            return new PublicInteriorCostTestController();
        }

        @Bean
        PublicCircleRateTestController publicCircleRateTestController() {
            return new PublicCircleRateTestController();
        }

        @Bean
        PublicStampDutyTestController publicStampDutyTestController() {
            return new PublicStampDutyTestController();
        }

        @Bean
        AdminInteriorCostTestController adminInteriorCostTestController() {
            return new AdminInteriorCostTestController();
        }

        @Bean
        PublicCitiesTestController publicCitiesTestController() {
            return new PublicCitiesTestController();
        }

        @Bean
        MobileUpdatePolicySecurityTestController mobileUpdatePolicySecurityTestController() {
            return new MobileUpdatePolicySecurityTestController();
        }

        @Bean
        PublicV2ProjectsTestController publicV2ProjectsTestController() {
            return new PublicV2ProjectsTestController();
        }

        @Bean
        PublicCmsContentTestController publicCmsContentTestController() {
            return new PublicCmsContentTestController();
        }

        @Bean
        FavoriteOverlayTestController favoriteOverlayTestController() {
            return new FavoriteOverlayTestController();
        }

        @Bean
        ActuatorSecurityTestController actuatorSecurityTestController() {
            return new ActuatorSecurityTestController();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        LogSanitizer logSanitizer() {
            return new LogSanitizer();
        }

        @Bean
        JwtRequestFilter jwtRequestFilter(LogSanitizer logSanitizer) {
            UserDetailsService userDetailsService = username -> {
                throw new UsernameNotFoundException(username);
            };
            // Not exposed as its own @Bean: JwtTokenUtil has @Value-bound fields with no
            // property source configured in this minimal context, so registering the mock
            // as a managed bean (rather than a plain local instance) would trigger Spring's
            // property injection on it and fail to resolve those placeholders.
            JwtTokenUtil jwtTokenUtil = Mockito.mock(JwtTokenUtil.class);
            return new JwtRequestFilter(userDetailsService, jwtTokenUtil, logSanitizer);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(LogSanitizer logSanitizer) {
            return new JwtAuthenticationEntryPoint(logSanitizer);
        }

        @Bean
        DashboardUserDetailsService dashboardUserDetailsService() {
            return Mockito.mock(DashboardUserDetailsService.class);
        }

        @Bean
        DashboardJwtAuthenticationFilter dashboardJwtAuthenticationFilter(
                DashboardUserDetailsService dashboardUserDetailsService,
                LogSanitizer logSanitizer
        ) {
            DashboardJwtService dashboardJwtService = Mockito.mock(DashboardJwtService.class);
            return new DashboardJwtAuthenticationFilter(dashboardJwtService, dashboardUserDetailsService, logSanitizer);
        }

        @Bean
        DashboardAuthenticationEntryPoint dashboardAuthenticationEntryPoint(
                ObjectMapper objectMapper,
                LogSanitizer logSanitizer
        ) {
            return new DashboardAuthenticationEntryPoint(objectMapper, logSanitizer);
        }

        @Bean
        DashboardAccessDeniedHandler dashboardAccessDeniedHandler(
                ObjectMapper objectMapper,
                LogSanitizer logSanitizer
        ) {
            return new DashboardAccessDeniedHandler(objectMapper, logSanitizer);
        }

        @Bean
        RateLimitProperties rateLimitProperties() {
            // Disabled here: these tests assert authorization rules, not rate limiting.
            RateLimitProperties properties = new RateLimitProperties();
            properties.setEnabled(false);
            return properties;
        }

        @Bean
        RateLimitService rateLimitService(RateLimitProperties rateLimitProperties) {
            return new InMemoryRateLimitService(rateLimitProperties);
        }

        @Bean
        RateLimitingFilter rateLimitingFilter(
                RateLimitProperties rateLimitProperties,
                RateLimitService rateLimitService,
                ObjectMapper objectMapper
        ) {
            // See jwtRequestFilter() above for why this mock is not its own @Bean.
            JwtTokenUtil jwtTokenUtil = Mockito.mock(JwtTokenUtil.class);
            return new RateLimitingFilter(
                    new RateLimitPolicyResolver(),
                    new RateLimitKeyResolver(),
                    new ClientIpResolver(rateLimitProperties),
                    rateLimitService,
                    rateLimitProperties,
                    objectMapper,
                    jwtTokenUtil
            );
        }

        @Bean
        PreAuthenticationAbuseFilter preAuthenticationAbuseFilter(
                RateLimitProperties rateLimitProperties,
                RateLimitService rateLimitService,
                ObjectMapper objectMapper
        ) {
            return new PreAuthenticationAbuseFilter(
                    new RateLimitPolicyResolver(), new ClientIpResolver(rateLimitProperties),
                    rateLimitService, rateLimitProperties, RateLimitMetrics.isolated(), objectMapper);
        }
    }
}
