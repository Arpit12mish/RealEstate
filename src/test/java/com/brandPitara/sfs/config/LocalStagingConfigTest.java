package com.brandPitara.sfs.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStagingConfigTest {

    private PropertySource<?> config() throws IOException {
        return new YamlPropertySourceLoader()
                .load("application-local-staging", new ClassPathResource("application-local-staging.yml"))
                .get(0);
    }

    @Test
    void alignsDatabaseAndServletLimitsWithProduction() throws IOException {
        PropertySource<?> config = config();

        assertThat(config.getProperty("spring.datasource.hikari.maximum-pool-size"))
                .isEqualTo("${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:10}");
        assertThat(config.getProperty("spring.datasource.hikari.minimum-idle"))
                .isEqualTo("${SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE:2}");
        assertThat(config.getProperty("spring.datasource.hikari.connection-timeout"))
                .isEqualTo("${SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT:5000}");
        assertThat(config.getProperty("spring.jpa.open-in-view")).isEqualTo(false);
        assertThat(config.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(config.getProperty("spring.flyway.clean-disabled")).isEqualTo(true);
        assertThat(config.getProperty("server.tomcat.threads.max"))
                .isEqualTo("${SERVER_TOMCAT_THREADS_MAX:50}");
        assertThat(config.getProperty("server.tomcat.mbeanregistry.enabled")).isEqualTo(true);
        assertThat(config.getProperty("server.forward-headers-strategy")).isEqualTo("none");
    }

    @Test
    void keepsProvidersSchedulersAndSeedsInert() throws IOException {
        PropertySource<?> config = config();

        assertThat(config.getProperty("sfs.search.enabled")).isEqualTo(false);
        assertThat(config.getProperty("google.maps.places.enabled")).isEqualTo(false);
        assertThat(config.getProperty("sfs.instagram.meta.sync-enabled")).isEqualTo(false);
        assertThat(config.getProperty("sfs.refresh-token.cleanup.enabled")).isEqualTo(false);
        assertThat(config.getProperty("dashboard.seed.enabled")).isEqualTo(false);
        assertThat(config.getProperty("app.review.enabled")).isEqualTo(false);
        assertThat(config.getProperty("sfs.local-staging.fake-otp.enabled"))
                .isEqualTo("${SFS_LOCAL_STAGING_FAKE_OTP_ENABLED:false}");
    }

    @Test
    void preservesBoundedCachesLoggingAndExactProxyTrust() throws IOException {
        PropertySource<?> config = config();

        assertThat(config.getProperty("sfs.rate-limit.trusted-proxies[0]"))
                .isEqualTo("${RATE_LIMIT_TRUSTED_PROXY_IP}");
        assertThat(config.getProperty("sfs.rate-limit.trusted-proxies[1]")).isNull();
        assertThat(config.getProperty("sfs.logging.request.async.queue-size"))
                .isEqualTo("${SFS_REQUEST_LOG_ASYNC_QUEUE_SIZE:2048}");
        assertThat(config.getProperty("sfs.authentication.identity-cache.mobile.maximum-size"))
                .isEqualTo("${SFS_AUTH_IDENTITY_CACHE_MOBILE_MAXIMUM_SIZE:50000}");
        assertThat(config.getProperty("sfs.authentication.identity-cache.dashboard.maximum-size"))
                .isEqualTo("${SFS_AUTH_IDENTITY_CACHE_DASHBOARD_MAXIMUM_SIZE:5000}");
    }
}
