package com.brandPitara.sfs.config;

import com.brandPitara.sfs.dashboard.auth.security.DashboardAccessDeniedHandler;
import com.brandPitara.sfs.dashboard.auth.security.DashboardAuthenticationEntryPoint;
import com.brandPitara.sfs.dashboard.auth.security.DashboardJwtAuthenticationFilter;
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

    private final JwtRequestFilter jwtRequestFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final DashboardJwtAuthenticationFilter dashboardJwtAuthenticationFilter;
    private final DashboardAuthenticationEntryPoint dashboardAuthenticationEntryPoint;
    private final DashboardAccessDeniedHandler dashboardAccessDeniedHandler;

    /**
     * Dashboard security chain.
     *
     * Handles only:
     * /api/dashboard/**
     *
     * Uses:
     * DashboardJwtAuthenticationFilter
     * DashboardAuthenticationEntryPoint
     * DashboardAccessDeniedHandler
     */
    @Bean
    @Order(1)
    public SecurityFilterChain dashboardFilterChain(HttpSecurity http) throws Exception {

        http
            .securityMatcher("/api/dashboard/**")
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(dashboardAuthenticationEntryPoint)
                .accessDeniedHandler(dashboardAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth

                // Public dashboard auth APIs
                .requestMatchers(HttpMethod.POST, "/api/dashboard/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/dashboard/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/dashboard/auth/logout").permitAll()

                // All other dashboard APIs require DASHBOARD JWT
                .anyRequest().authenticated()
            );

        http.addFilterBefore(
            dashboardJwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    /**
     * Existing app/mobile security chain.
     *
     * Handles everything except /api/dashboard/**.
     *
     * Uses:
     * JwtRequestFilter
     * JwtAuthenticationEntryPoint
     */
    @Bean
    @Order(2)
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth

                // Public health & docs
                .requestMatchers(
                    "/api/health",
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Existing mobile auth APIs
                .requestMatchers("/api/auth/**").permitAll()

                // Public location resolve API
                .requestMatchers(HttpMethod.POST, "/api/location/resolve").permitAll()

                // Public city APIs for manual city selector/search
                .requestMatchers(HttpMethod.GET, "/api/cities/**").permitAll()

                // Public app content APIs
                .requestMatchers(HttpMethod.GET, "/api/app-content/**").permitAll()

                // Public provider APIs
                .requestMatchers(HttpMethod.GET, "/api/providers/**").permitAll()

                // Public listing / search / calculators
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/public/**",
                    "/api/projects/**",
                    "/api/businesses/**",
                    "/api/public/stamp-duty/**",
                    "/api/public/interior-cost/**",
                    "/api/search/**"
                ).permitAll()

                // Everything else requires existing USER/GUEST JWT
                .anyRequest().authenticated()
            );

        http.addFilterBefore(
            jwtRequestFilter,
            UsernamePasswordAuthenticationFilter.class
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