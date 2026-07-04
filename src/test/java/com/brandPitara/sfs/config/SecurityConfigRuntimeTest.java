package com.brandPitara.sfs.config;

import com.brandPitara.sfs.dashboard.auth.security.DashboardAccessDeniedHandler;
import com.brandPitara.sfs.dashboard.auth.security.DashboardAuthenticationEntryPoint;
import com.brandPitara.sfs.dashboard.auth.security.DashboardJwtAuthenticationFilter;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetailsService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardJwtService;
import com.brandPitara.sfs.observability.LogSanitizer;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigRuntimeTest {

    private MockMvc mockMvc;
    private AnnotationConfigWebApplicationContext context;

    @BeforeEach
    void setUp() {
        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        webContext.setServletContext(new MockServletContext());
        webContext.register(TestMvcConfig.class);
        webContext.refresh();

        Filter springSecurityFilterChain = webContext.getBean("springSecurityFilterChain", Filter.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext)
                .addFilters(springSecurityFilterChain)
                .build();
        context = webContext;
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
    void appScreenContentIsPublicAtRuntime() throws Exception {
        mockMvc.perform(get("/api/app/screen-content"))
                .andExpect(status().isOk());
    }

    @Test
    void projectsCompareIsPublicAtRuntime() throws Exception {
        mockMvc.perform(post("/api/projects/compare"))
                .andExpect(status().isOk());
    }

    @Test
    void unrelatedAppAndProjectEndpointsRemainProtectedAtRuntime() throws Exception {
        mockMvc.perform(get("/api/app/other-content"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects/other"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    @RequestMapping("/api/auth")
    static class MobileAuthTestController {

        @PostMapping("/request-otp")
        ResponseEntity<Void> requestOtp() {
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
        AppContentTestController appContentTestController() {
            return new AppContentTestController();
        }

        @Bean
        ProjectsTestController projectsTestController() {
            return new ProjectsTestController();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
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
    }
}
