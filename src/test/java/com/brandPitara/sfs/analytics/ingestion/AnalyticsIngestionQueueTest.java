package com.brandPitara.sfs.analytics.ingestion;

import com.brandPitara.sfs.analytics.config.AnalyticsIngestionProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Production-hardening pass (Part B): proves the queue's non-blocking contract and
 * its Micrometer instrumentation, using a mocked repository whose insertBatch() call
 * is latch-controlled so tests are deterministic rather than sleep-based races.
 */
class AnalyticsIngestionQueueTest {

    private AnalyticsIngestionQueue queue;

    @AfterEach
    void tearDown() {
        if (queue != null) {
            queue.stop();
        }
    }

    private AnalyticsIngestionProperties propertiesWithCapacity(int capacity) {
        AnalyticsIngestionProperties properties = new AnalyticsIngestionProperties();
        properties.setQueueCapacity(capacity);
        properties.setDrainBatchSize(Math.max(1, Math.min(capacity, 50)));
        properties.setDrainIntervalMs(100);
        return properties;
    }

    private AnalyticsEventRecord sampleRecord() {
        Instant now = Instant.now();
        return new AnalyticsEventRecord(
                UUID.randomUUID(), now, now, (short) 1, "SEARCH_SUBMITTED",
                UUID.randomUUID(), null, UUID.randomUUID(), "SEARCH", null, null,
                null, null, null, "ANDROID", "1.0.0", null
        );
    }

    private void awaitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(condition.getAsBoolean()).as("condition met within %dms", timeoutMs).isTrue();
    }

    @Test
    void acceptedCounterIncrementsOnEnqueueAndInsertedIncrementsOnceDrained() throws InterruptedException {
        AnalyticsEventJdbcRepository repo = mock(AnalyticsEventJdbcRepository.class);
        when(repo.insertBatch(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        queue = new AnalyticsIngestionQueue(propertiesWithCapacity(100), repo, registry);
        queue.start();

        queue.enqueue(sampleRecord());

        assertThat(queue.getAcceptedCount()).isEqualTo(1);
        assertThat(registry.get("analytics.events.accepted").counter().count()).isEqualTo(1.0);

        awaitUntil(() -> queue.getPersistedCount() >= 1, 2_000);
        assertThat(registry.get("analytics.batch.inserted").counter().count()).isEqualTo(1.0);
    }

    @Test
    void fullQueueDropsWithoutBlockingCaller() throws InterruptedException {
        CountDownLatch insertStarted = new CountDownLatch(1);
        CountDownLatch releaseInsert = new CountDownLatch(1);
        AnalyticsEventJdbcRepository repo = mock(AnalyticsEventJdbcRepository.class);
        when(repo.insertBatch(anyList())).thenAnswer(inv -> {
            insertStarted.countDown();
            releaseInsert.await(5, TimeUnit.SECONDS);
            return ((List<?>) inv.getArgument(0)).size();
        });

        AnalyticsIngestionProperties properties = propertiesWithCapacity(2);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        queue = new AnalyticsIngestionQueue(properties, repo, registry);
        queue.start();
        try {
            // Picked up by the drain loop immediately; the worker then blocks inside
            // insertBatch, so the queue is guaranteed empty again while we fill it.
            queue.enqueue(sampleRecord());
            assertThat(insertStarted.await(2, TimeUnit.SECONDS)).isTrue();

            long startNanos = System.nanoTime();
            queue.enqueue(sampleRecord());
            queue.enqueue(sampleRecord());
            queue.enqueue(sampleRecord()); // capacity is 2 - this one must be dropped
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

            assertThat(elapsedMs)
                    .as("enqueue on a full queue must never block the calling thread")
                    .isLessThan(1_000);
            assertThat(queue.getDroppedCount()).isGreaterThanOrEqualTo(1);
            assertThat(registry.get("analytics.events.dropped").counter().count()).isGreaterThanOrEqualTo(1.0);
        } finally {
            releaseInsert.countDown();
        }
    }

    @Test
    void queueDepthGaugeReflectsCurrentSize() throws InterruptedException {
        CountDownLatch insertStarted = new CountDownLatch(1);
        CountDownLatch releaseInsert = new CountDownLatch(1);
        AnalyticsEventJdbcRepository repo = mock(AnalyticsEventJdbcRepository.class);
        when(repo.insertBatch(anyList())).thenAnswer(inv -> {
            insertStarted.countDown();
            releaseInsert.await(5, TimeUnit.SECONDS);
            return ((List<?>) inv.getArgument(0)).size();
        });

        AnalyticsIngestionProperties properties = propertiesWithCapacity(10);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        queue = new AnalyticsIngestionQueue(properties, repo, registry);
        queue.start();
        try {
            queue.enqueue(sampleRecord());
            assertThat(insertStarted.await(2, TimeUnit.SECONDS)).isTrue();
            // The single enqueued event was already drained into the blocked batch;
            // the queue itself is empty again, so these three are the only ones in it.
            queue.enqueue(sampleRecord());
            queue.enqueue(sampleRecord());
            queue.enqueue(sampleRecord());

            assertThat(registry.get("analytics.queue.depth").gauge().value()).isEqualTo(3.0);
            assertThat(registry.get("analytics.queue.capacity").gauge().value()).isEqualTo(10.0);
        } finally {
            releaseInsert.countDown();
        }
    }

    @Test
    void workerSurvivesRepositoryExceptionAndKeepsProcessingAfterwards() throws InterruptedException {
        AnalyticsEventJdbcRepository repo = mock(AnalyticsEventJdbcRepository.class);
        when(repo.insertBatch(anyList()))
                .thenThrow(new RuntimeException("simulated db failure"))
                .thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());

        AnalyticsIngestionProperties properties = propertiesWithCapacity(10);
        properties.setDrainIntervalMs(50);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        queue = new AnalyticsIngestionQueue(properties, repo, registry);
        queue.start();

        queue.enqueue(sampleRecord());
        awaitUntil(() -> queue.getFailedBatchCount() >= 1, 2_000);
        assertThat(registry.get("analytics.batch.failed").counter().count()).isGreaterThanOrEqualTo(1.0);

        // The worker thread must still be alive and processing after the failure.
        queue.enqueue(sampleRecord());
        awaitUntil(() -> queue.getPersistedCount() >= 1, 2_000);
        assertThat(registry.get("analytics.batch.inserted").counter().count()).isGreaterThanOrEqualTo(1.0);
    }
}
