package com.brandPitara.sfs.scheduler;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenCleanupSchedulerTest {

    @Test
    void stopsAtMaximumBatchCount() {
        RefreshTokenCleanupBatchWorker worker = mock(RefreshTokenCleanupBatchWorker.class);
        when(worker.deleteBatch(any(), anyInt())).thenReturn(100, 100, 0);
        RefreshTokenCleanupProperties properties = properties(100, 2, Duration.ofMinutes(1));

        RefreshTokenCleanupScheduler.CleanupRunResult result = scheduler(worker, properties).runCleanup();

        assertThat(result.completedBatches()).isEqualTo(2);
        assertThat(result.deletedRows()).isEqualTo(200);
        assertThat(result.stopReason()).isEqualTo(RefreshTokenCleanupScheduler.StopReason.MAXIMUM_BATCHES);
        verify(worker, org.mockito.Mockito.times(2)).deleteBatch(any(), anyInt());
    }

    @Test
    void stopsAtMaximumDurationAfterCommittingCompletedBatch() {
        RefreshTokenCleanupBatchWorker worker = mock(RefreshTokenCleanupBatchWorker.class);
        when(worker.deleteBatch(any(), anyInt())).thenReturn(100);
        RefreshTokenCleanupProperties properties = properties(100, 10, Duration.ofMillis(5));
        LongSupplier time = sequence(0, 1_000_000, 6_000_000, 7_000_000);

        RefreshTokenCleanupScheduler.CleanupRunResult result =
                new RefreshTokenCleanupScheduler(worker, properties, new SimpleMeterRegistry(), time).runCleanup();

        assertThat(result.completedBatches()).isOne();
        assertThat(result.deletedRows()).isEqualTo(100);
        assertThat(result.stopReason()).isEqualTo(RefreshTokenCleanupScheduler.StopReason.MAXIMUM_DURATION);
        verify(worker).deleteBatch(any(), anyInt());
    }

    @Test
    void failureStopsRunAndFollowingRunCanRecover() {
        RefreshTokenCleanupBatchWorker worker = mock(RefreshTokenCleanupBatchWorker.class);
        when(worker.deleteBatch(any(), anyInt()))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(0);
        RefreshTokenCleanupScheduler scheduler = scheduler(
                worker,
                properties(100, 10, Duration.ofMinutes(1))
        );

        RefreshTokenCleanupScheduler.CleanupRunResult failed = scheduler.runCleanup();
        RefreshTokenCleanupScheduler.CleanupRunResult recovered = scheduler.runCleanup();

        assertThat(failed.stopReason()).isEqualTo(RefreshTokenCleanupScheduler.StopReason.FAILURE);
        assertThat(failed.completedBatches()).isZero();
        assertThat(recovered.stopReason()).isEqualTo(RefreshTokenCleanupScheduler.StopReason.EXHAUSTED);
        assertThat(recovered.completedBatches()).isOne();
    }

    @Test
    void preventsSameJvmOverlap() throws Exception {
        RefreshTokenCleanupBatchWorker worker = mock(RefreshTokenCleanupBatchWorker.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(worker.deleteBatch(any(), anyInt())).thenAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return 0;
        });
        RefreshTokenCleanupScheduler scheduler = scheduler(
                worker,
                properties(100, 10, Duration.ofMinutes(1))
        );
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<RefreshTokenCleanupScheduler.CleanupRunResult> first = executor.submit(scheduler::runCleanup);
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        RefreshTokenCleanupScheduler.CleanupRunResult overlapping = scheduler.runCleanup();
        release.countDown();

        assertThat(first.get(5, TimeUnit.SECONDS).stopReason())
                .isEqualTo(RefreshTokenCleanupScheduler.StopReason.EXHAUSTED);
        assertThat(overlapping.stopReason()).isEqualTo(RefreshTokenCleanupScheduler.StopReason.OVERLAP);
        executor.shutdownNow();
    }

    @Test
    void configurationRejectsUnsafeValuesAndInvalidCron() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        RefreshTokenCleanupProperties properties = new RefreshTokenCleanupProperties();
        properties.setBatchSize(0);
        properties.setMaximumBatchesPerRun(0);
        properties.setMaximumExecutionDuration(Duration.ZERO);
        properties.setSchedule("not-a-cron");

        assertThat(validator.validate(properties)).hasSize(4);
    }

    private RefreshTokenCleanupScheduler scheduler(
            RefreshTokenCleanupBatchWorker worker,
            RefreshTokenCleanupProperties properties
    ) {
        return new RefreshTokenCleanupScheduler(
                worker,
                properties,
                new SimpleMeterRegistry(),
                System::nanoTime
        );
    }

    private RefreshTokenCleanupProperties properties(int batchSize, int maximumBatches, Duration duration) {
        RefreshTokenCleanupProperties properties = new RefreshTokenCleanupProperties();
        properties.setBatchSize(batchSize);
        properties.setMaximumBatchesPerRun(maximumBatches);
        properties.setMaximumExecutionDuration(duration);
        return properties;
    }

    private LongSupplier sequence(long... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[Math.min(index.getAndIncrement(), values.length - 1)];
    }
}
