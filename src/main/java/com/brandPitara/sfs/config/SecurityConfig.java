package com.brandPitara.sfs.config;

import com.brandPitara.sfs.dashboard.auth.security.DashboardAccessDeniedHandler;
import com.brandPitara.sfs.dashboard.auth.security.DashboardAuthenticationEntryPoint;
import com.brandPitara.sfs.dashboard.auth.security.DashboardJwtAuthenticationFilter;
import com.brandPitara.sfs.ratelimit.filter.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    static final String[] PUBLIC_MOBILE_AUTH_ENDPOINTS = {
            "/api/auth/request-otp",
            "/api/auth/verify-otp",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/logout-all",
            "/api/auth/guest/session"
    };

    private final JwtRequestFilter jwtRequestFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final DashboardJwtAuthenticationFilter dashboardJwtAuthenticationFilter;
    private final DashboardAuthenticationEntryPoint dashboardAuthenticationEntryPoint;
    private final DashboardAccessDeniedHandler dashboardAccessDeniedHandler;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain dashboardFilterChain(HttpSecurity http) throws Exception {

        http
            .securityMatcher("/api/dashboard/**", "/api/admin/**")
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(dashboardAuthenticationEntryPoint)
                .accessDeniedHandler(dashboardAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/dashboard/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/dashboard/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/dashboard/auth/logout").permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(
            dashboardJwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(
                    "/api/health",
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_MOBILE_AUTH_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/location/resolve").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cities/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/app-content/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/app/screen-content").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/providers/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/builders/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/projects/compare").permitAll()
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/public/**",
                    "/api/projects/**",
                    "/api/businesses/**",
                    "/api/public/stamp-duty/**",
                    "/api/public/interior-cost/**",
                    "/api/search/**"
                ).permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(
            jwtRequestFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        // Runs after JWT authentication so SecurityContext is already populated
        // for optionally-authenticated public endpoints (e.g. PUBLIC_PROJECT_COMPARE's
        // IP_OR_USER key), and before any controller logic executes. Mobile/public
        // routes only for Phase 1 - dashboard rate limiting is not in scope here.
        http.addFilterAfter(
            rateLimitingFilter,
            JwtRequestFilter.class
        );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
