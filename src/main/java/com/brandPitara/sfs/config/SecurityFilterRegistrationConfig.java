package com.brandPitara.sfs.config;

import com.brandPitara.sfs.dashboard.auth.security.DashboardJwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Security filters are installed only in their explicit Spring Security chains. */
@Configuration
public class SecurityFilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<JwtRequestFilter> jwtRequestFilterRegistration(JwtRequestFilter filter) {
        FilterRegistrationBean<JwtRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<DashboardJwtAuthenticationFilter> dashboardJwtFilterRegistration(
            DashboardJwtAuthenticationFilter filter) {
        FilterRegistrationBean<DashboardJwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
