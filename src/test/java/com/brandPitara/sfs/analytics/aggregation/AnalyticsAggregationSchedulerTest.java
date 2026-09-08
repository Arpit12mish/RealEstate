package com.brandPitara.sfs.analytics.aggregation;

import com.brandPitara.sfs.analytics.config.AnalyticsIngestionProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Backend-optimization audit (Part 4/7): two aggregation runs overlapping - the
 * scheduled nightly run and a manual backfill (AnalyticsDashboardController#run
 * Aggregation, which executes on an HTTP worker thread, not the shared @Scheduled
 * thread) - would both full-scan the same partition and race wastefully on the same
 * ON CONFLICT rows. Proves the AtomicBoolean guard actually serializes them.
 */
class AnalyticsAggregationSchedulerTest {

    @Test
    void secondConcurrentRunIsSkippedWhileFirstIsInProgress() throws InterruptedException {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object.class), any(Object.class))).thenAnswer(inv -> {
            firstStarted.countDown();
            releaseFirst.await(5, TimeUnit.SECONDS);
            return 0;
        });

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AnalyticsAggregationScheduler scheduler =
                new AnalyticsAggregationScheduler(jdbcTemplate, new AnalyticsIngestionProperties(), registry);
        invokeInit(scheduler);

        Thread firstRun = new Thread(() -> scheduler.runFor(LocalDate.now()));
        firstRun.start();
        try {
            assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();

            // Second call, from "another thread" conceptually (the test's main thread
            // here, standing in for an HTTP worker thread) - must return immediately,
            // not block waiting for the first to finish.
            long startNanos = System.nanoTime();
            scheduler.runFor(LocalDate.now());
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

            assertThat(elapsedMs).as("overlapping call must be skipped, not blocked").isLessThan(500);
            assertThat(registry.get("analytics.aggregation.skipped").counter().count()).isEqualTo(1.0);
            assertThat(registry.get("analytics.aggregation.completed").counter().count()).isZero();
        } finally {
            releaseFirst.countDown();
            firstRun.join(5_000);
        }

        assertThat(registry.get("analytics.aggregation.completed").counter().count()).isEqualTo(1.0);
    }

    @Test
    void sequentialRunsBothCompleteNormally() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object.class), any(Object.class))).thenReturn(0);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AnalyticsAggregationScheduler scheduler =
                new AnalyticsAggregationScheduler(jdbcTemplate, new AnalyticsIngestionProperties(), registry);
        invokeInit(scheduler);

        scheduler.runFor(LocalDate.now().minusDays(1));
        scheduler.runFor(LocalDate.now());

        assertThat(registry.get("analytics.aggregation.completed").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("analytics.aggregation.skipped").counter().count()).isZero();
    }

    private void invokeInit(AnalyticsAggregationScheduler scheduler) {
        try {
            var method = AnalyticsAggregationScheduler.class.getDeclaredMethod("init");
            method.setAccessible(true);
            method.invoke(scheduler);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
