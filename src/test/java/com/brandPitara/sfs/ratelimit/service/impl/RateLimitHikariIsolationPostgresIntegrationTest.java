package com.brandPitara.sfs.ratelimit.service.impl;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RateLimitHikariIsolationPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_rate_limit")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @Test
    void rateLimitOnlyTrafficNeverChecksOutAPostgresConnection() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(postgres.getJdbcUrl());
        hikari.setUsername(postgres.getUsername());
        hikari.setPassword(postgres.getPassword());
        hikari.setMaximumPoolSize(1);
        hikari.setMinimumIdle(0);
        hikari.setConnectionTimeout(1_000);
        hikari.setPoolName("rate-limit-isolation");
        hikari.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));

        try (HikariDataSource dataSource = new HikariDataSource(hikari)) {
            try (var ignored = dataSource.getConnection()) {
                // Initialize the constrained pool, then prove limiter traffic leaves it untouched.
            }
            HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
            awaitReturned(pool);
            Counter timeoutCounter = registry.find("hikaricp.connections.timeout").counter();
            double beforeTimeouts = timeoutCounter == null ? 0 : timeoutCounter.count();

            InMemoryRateLimitService service = service();
            for (int index = 0; index < 20_000; index++) {
                String ip = "203.0.113." + (index % 250 + 1);
                service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ, Map.of(
                        RateLimitKeyType.PRIMARY_IDENTITY, "anonymous:" + ip,
                        RateLimitKeyType.IP_ABUSE, ip));
            }

            Counter afterCounter = registry.find("hikaricp.connections.timeout").counter();
            double afterTimeouts = afterCounter == null ? 0 : afterCounter.count();
            assertThat(pool.getActiveConnections()).isZero();
            assertThat(pool.getThreadsAwaitingConnection()).isZero();
            assertThat(afterTimeouts - beforeTimeouts).isZero();
        }
    }

    private InMemoryRateLimitService service() {
        RateLimitProperties properties = new RateLimitProperties();
        RateLimitProperties.LimitConfig primary = limit(RateLimitKeyType.PRIMARY_IDENTITY, 100_000);
        RateLimitProperties.LimitConfig abuse = limit(RateLimitKeyType.IP_ABUSE, 1_000_000);
        RateLimitProperties.PolicyConfig policy = new RateLimitProperties.PolicyConfig();
        policy.setLimits(List.of(primary, abuse));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy);
        return new InMemoryRateLimitService(properties);
    }

    private RateLimitProperties.LimitConfig limit(RateLimitKeyType type, long capacity) {
        RateLimitProperties.LimitConfig limit = new RateLimitProperties.LimitConfig();
        limit.setKeyType(type);
        limit.setCapacity(capacity);
        limit.setRefillTokens(capacity);
        limit.setRefillPeriodSeconds(60);
        return limit;
    }

    private void awaitReturned(HikariPoolMXBean pool) throws InterruptedException {
        long deadline = System.nanoTime() + 2_000_000_000L;
        while (pool.getActiveConnections() != 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
    }
}
