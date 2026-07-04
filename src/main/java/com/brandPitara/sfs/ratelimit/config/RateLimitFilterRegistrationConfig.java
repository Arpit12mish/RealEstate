package com.brandPitara.sfs.ratelimit.config;

import com.brandPitara.sfs.ratelimit.filter.RateLimitingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RateLimitingFilter is a {@code @Component} so it can be constructor-injected
 * into SecurityConfig and added explicitly via httpSecurity.addFilterAfter(...).
 * Without this bean, Spring Boot would ALSO auto-register it as a generic
 * servlet-container filter (since any Filter bean is picked up for that by
 * default), causing it to run twice per request and double-consume rate-limit
 * tokens. Disabling the generic auto-registration keeps it running exactly
 * once, inside the app security filter chain.
 */
@Configuration
public class RateLimitFilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
