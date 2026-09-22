package com.brandPitara.sfs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Split out of {@link SecurityConfig} so {@code PasswordEncoder} stays available under a
 * non-servlet boot (see {@link SecurityConfig}'s {@code @ConditionalOnWebApplication}) - unlike
 * the two {@code SecurityFilterChain} beans, nothing about password hashing requires an
 * {@code HttpSecurity}/servlet context. Dashboard auth/user-management services depend on this
 * bean unconditionally and would otherwise fail to construct whenever {@code SecurityConfig}
 * itself is skipped.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
