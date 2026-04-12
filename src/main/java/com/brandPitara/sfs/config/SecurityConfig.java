package com.brandPitara.sfs.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth

                // 🔓 Public health & docs
                .requestMatchers(
                    "/api/health",
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // 🔓 Auth-related APIs
                .requestMatchers("/api/auth/**").permitAll()

                // 🔓 Public app content APIs (privacy / terms / about / contact / share links)
                .requestMatchers(HttpMethod.GET, "/api/app-content/**").permitAll()

                // 🔓 Public provider APIs
                .requestMatchers(HttpMethod.GET, "/api/providers/**").permitAll()

                // 🔓 Public listing / search / calculators
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/public/**",
                    "/api/projects/**",
                    "/api/businesses/**",
                    "/api/public/stamp-duty/**",
                    "/api/public/interior-cost/**",
                    "/api/search/**"
                ).permitAll()

                // 🔐 Everything else requires JWT
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

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