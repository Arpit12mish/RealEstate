package com.brandPitara.sfs.incident;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.brandPitara.sfs.buildercredibility.service.impl.BuilderCredibilityServiceImpl;
import com.brandPitara.sfs.projectmeter.service.impl.ProjectMeterServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic reproduction of the connection ownership pattern that existed in
 * ProjectServiceImpl.publicGet -> ProjectMeterServiceImpl.publicGetMeterDetail.
 * Each request owns an outer connection and then asks REQUIRES_NEW for another one.
 */
@Testcontainers(disabledWithoutDocker = true)
class NestedRequiresNewPoolStarvationReproductionTest {

    private static final int POOL_SIZE = 3;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("hikari_incident")
            .withUsername("incident_test")
            .withPassword("incident_test");

    @Test
    void requiresNewInnerWorkStarvesWhenEveryOuterTransactionOwnsAConnection() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(POOL_SIZE);
        config.setMinimumIdle(POOL_SIZE);
        config.setConnectionTimeout(500);
        config.setPoolName("IncidentReproductionPool");

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            assertThat(waitForPoolSize(dataSource, Duration.ofSeconds(5))).isTrue();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            TransactionTemplate outer = new TransactionTemplate(transactionManager);
            outer.setReadOnly(true);
            TransactionTemplate inner = new TransactionTemplate(transactionManager);
            inner.setReadOnly(true);
            inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            CountDownLatch outerConnectionsAcquired = new CountDownLatch(POOL_SIZE);
            CountDownLatch startInnerWork = new CountDownLatch(1);
            AtomicInteger acquisitionTimeouts = new AtomicInteger();
            ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
            List<Future<?>> requests = new ArrayList<>();

            for (int i = 0; i < POOL_SIZE; i++) {
                requests.add(executor.submit(() -> outer.executeWithoutResult(status -> {
                    jdbc.queryForObject("select 1", Integer.class);
                    outerConnectionsAcquired.countDown();
                    await(startInnerWork);
                    try {
                        inner.executeWithoutResult(ignored -> jdbc.queryForObject("select 1", Integer.class));
                    } catch (RuntimeException expected) {
                        acquisitionTimeouts.incrementAndGet();
                    }
                })));
            }

            assertThat(outerConnectionsAcquired.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(dataSource.getHikariPoolMXBean().getActiveConnections()).isEqualTo(POOL_SIZE);
            startInnerWork.countDown();

            assertThat(waitForPending(dataSource, Duration.ofSeconds(2))).isEqualTo(POOL_SIZE);
            for (Future<?> request : requests) {
                request.get(5, TimeUnit.SECONDS);
            }

            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(acquisitionTimeouts.get()).isGreaterThanOrEqualTo(1);
            System.out.printf("requires-new-reproduction active=%d pending=%d timeouts=%d%n",
                    POOL_SIZE, POOL_SIZE, acquisitionTimeouts.get());
            assertThat(waitForPoolToDrain(dataSource, Duration.ofSeconds(2))).isTrue();
            assertThat(dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
        }
    }

    @Test
    void projectMeterDetailJoinsTheOuterTransactionInsteadOfBorrowingASecondConnection() throws Exception {
        Method method = ProjectMeterServiceImpl.class.getMethod("publicGetMeterDetail", Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRED);

        Method credibilityMethod = BuilderCredibilityServiceImpl.class
                .getMethod("publicGetCredibilitySummary", Long.class);
        Transactional credibilityTransactional = credibilityMethod.getAnnotation(Transactional.class);
        assertThat(credibilityTransactional).isNotNull();
        assertThat(credibilityTransactional.readOnly()).isTrue();
        assertThat(credibilityTransactional.propagation()).isEqualTo(Propagation.REQUIRED);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(POOL_SIZE);
        config.setMinimumIdle(POOL_SIZE);
        config.setConnectionTimeout(500);
        config.setPoolName("IncidentRegressionPool");

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            assertThat(waitForPoolSize(dataSource, Duration.ofSeconds(5))).isTrue();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            TransactionTemplate outer = new TransactionTemplate(transactionManager);
            outer.setReadOnly(true);
            TransactionTemplate inner = new TransactionTemplate(transactionManager);
            inner.setReadOnly(true);
            inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

            for (int repetition = 0; repetition < 3; repetition++) {
                CountDownLatch outerConnectionsAcquired = new CountDownLatch(POOL_SIZE);
                CountDownLatch startInnerWork = new CountDownLatch(1);
                AtomicInteger failures = new AtomicInteger();
                ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
                List<Future<?>> requests = new ArrayList<>();

                for (int i = 0; i < POOL_SIZE; i++) {
                    requests.add(executor.submit(() -> outer.executeWithoutResult(status -> {
                        jdbc.queryForObject("select 1", Integer.class);
                        outerConnectionsAcquired.countDown();
                        await(startInnerWork);
                        try {
                            inner.executeWithoutResult(ignored -> jdbc.queryForObject("select 1", Integer.class));
                        } catch (RuntimeException unexpected) {
                            failures.incrementAndGet();
                        }
                    })));
                }

                assertThat(outerConnectionsAcquired.await(10, TimeUnit.SECONDS)).isTrue();
                startInnerWork.countDown();
                for (Future<?> request : requests) {
                    request.get(5, TimeUnit.SECONDS);
                }
                executor.shutdown();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
                assertThat(failures).hasValue(0);
                assertThat(dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
                assertThat(waitForPoolToDrain(dataSource, Duration.ofSeconds(2))).isTrue();
            }
        }
    }

    @Test
    void tenConcurrentDevicesCompleteRepeatedlyWithAThreeConnectionPool() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(POOL_SIZE);
        config.setMinimumIdle(POOL_SIZE);
        config.setConnectionTimeout(2_000);
        config.setPoolName("TenDeviceRegressionPool");

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            assertThat(waitForPoolSize(dataSource, Duration.ofSeconds(5))).isTrue();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            TransactionTemplate outer = new TransactionTemplate(transactionManager);
            outer.setReadOnly(true);
            TransactionTemplate inner = new TransactionTemplate(transactionManager);
            inner.setReadOnly(true);
            inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

            for (int repetition = 0; repetition < 3; repetition++) {
                CountDownLatch start = new CountDownLatch(1);
                AtomicInteger failures = new AtomicInteger();
                AtomicInteger peakActive = new AtomicInteger();
                List<Long> durationsMillis = Collections.synchronizedList(new ArrayList<>());
                ExecutorService executor = Executors.newFixedThreadPool(10);
                List<Future<?>> requests = new ArrayList<>();

                for (int device = 0; device < 10; device++) {
                    requests.add(executor.submit(() -> {
                        await(start);
                        long started = System.nanoTime();
                        try {
                            outer.executeWithoutResult(status -> {
                                jdbc.queryForObject("select 1 from pg_sleep(0.02)", Integer.class);
                                peakActive.accumulateAndGet(
                                        dataSource.getHikariPoolMXBean().getActiveConnections(), Math::max);
                                inner.executeWithoutResult(ignored ->
                                        jdbc.queryForObject("select 1 from pg_sleep(0.02)", Integer.class));
                            });
                        } catch (RuntimeException unexpected) {
                            failures.incrementAndGet();
                        } finally {
                            durationsMillis.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                        }
                    }));
                }

                start.countDown();
                for (Future<?> request : requests) {
                    request.get(5, TimeUnit.SECONDS);
                }
                executor.shutdown();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

                durationsMillis.sort(Long::compareTo);
                long p95Millis = durationsMillis.get(9);
                System.out.printf(
                        "ten-device repetition=%d peakActive=%d endPending=%d timeouts=%d p95Ms=%d%n",
                        repetition + 1,
                        peakActive.get(),
                        dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
                        failures.get(),
                        p95Millis);
                assertThat(failures).hasValue(0);
                assertThat(peakActive).hasValue(POOL_SIZE);
                assertThat(p95Millis).isLessThan(2_000);
                assertThat(dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
                assertThat(waitForPoolToDrain(dataSource, Duration.ofSeconds(2))).isTrue();
            }
        }
    }

    private int waitForPending(HikariDataSource dataSource, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        int peak = 0;
        while (System.nanoTime() < deadline) {
            peak = Math.max(peak, dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            if (peak == POOL_SIZE) {
                return peak;
            }
            Thread.sleep(10);
        }
        return peak;
    }

    private boolean waitForPoolToDrain(HikariDataSource dataSource, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (dataSource.getHikariPoolMXBean().getActiveConnections() == 0) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    private boolean waitForPoolSize(HikariDataSource dataSource, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (dataSource.getHikariPoolMXBean().getTotalConnections() == POOL_SIZE) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent requests");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
