package com.brandPitara.sfs.scheduler;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

@Component
@Slf4j
public class RefreshTokenCleanupScheduler {

    private static final String METRIC_PREFIX = "sfs.refresh_token.cleanup";

    private final RefreshTokenCleanupBatchWorker worker;
    private final RefreshTokenCleanupProperties properties;
    private final MeterRegistry meterRegistry;
    private final LongSupplier nanoTime;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public RefreshTokenCleanupScheduler(
            RefreshTokenCleanupBatchWorker worker,
            RefreshTokenCleanupProperties properties,
            MeterRegistry meterRegistry
    ) {
        this(worker, properties, meterRegistry, System::nanoTime);
    }

    RefreshTokenCleanupScheduler(
            RefreshTokenCleanupBatchWorker worker,
            RefreshTokenCleanupProperties properties,
            MeterRegistry meterRegistry,
            LongSupplier nanoTime
    ) {
        this.worker = worker;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.nanoTime = nanoTime;
    }

    @Scheduled(cron = "${sfs.refresh-token.cleanup.schedule:0 0 2 * * *}")
    public void cleanup() {
        runCleanup();
    }

    CleanupRunResult runCleanup() {
        if (!properties.isEnabled()) {
            return CleanupRunResult.notRun(StopReason.DISABLED);
        }

        if (!running.compareAndSet(false, true)) {
            counter("runs", "outcome", "overlap_skipped").increment();
            log.info("event=refresh_token_cleanup outcome=overlap_skipped");
            return CleanupRunResult.notRun(StopReason.OVERLAP);
        }

        long started = nanoTime.getAsLong();
        int batches = 0;
        int deletedRows = 0;
        StopReason reason = StopReason.EXHAUSTED;
        boolean exhausted = false;

        try {
            OffsetDateTime cutoff = OffsetDateTime.now();
            while (batches < properties.getMaximumBatchesPerRun()) {
                if (elapsedNanos(started) >= properties.getMaximumExecutionDuration().toNanos()) {
                    reason = StopReason.MAXIMUM_DURATION;
                    break;
                }

                int deleted;
                try {
                    deleted = worker.deleteBatch(cutoff, properties.getBatchSize());
                } catch (RuntimeException ex) {
                    reason = StopReason.FAILURE;
                    counter("runs", "outcome", "failure").increment();
                    log.warn(
                            "event=refresh_token_cleanup outcome=failure completedBatches={} deletedRows={} errorType={}",
                            batches,
                            deletedRows,
                            ex.getClass().getSimpleName()
                    );
                    return result(started, batches, deletedRows, reason);
                }

                batches++;
                deletedRows += deleted;
                counter("batches", "outcome", "committed").increment();
                counter("rows", "outcome", "deleted").increment(deleted);

                if (deleted < properties.getBatchSize()) {
                    reason = StopReason.EXHAUSTED;
                    exhausted = true;
                    break;
                }
            }

            if (!exhausted && batches >= properties.getMaximumBatchesPerRun()) {
                reason = StopReason.MAXIMUM_BATCHES;
            }

            CleanupRunResult result = result(started, batches, deletedRows, reason);
            counter("runs", "outcome", reason.metricValue()).increment();
            log.info(
                    "event=refresh_token_cleanup outcome={} completedBatches={} deletedRows={} durationMs={}",
                    reason.metricValue(),
                    batches,
                    deletedRows,
                    result.durationMillis()
            );
            return result;
        } finally {
            running.set(false);
        }
    }

    private CleanupRunResult result(long started, int batches, int deletedRows, StopReason reason) {
        long durationNanos = elapsedNanos(started);
        Timer.builder(METRIC_PREFIX + ".duration")
                .tag("outcome", reason.metricValue())
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
        return new CleanupRunResult(batches, deletedRows, reason, TimeUnit.NANOSECONDS.toMillis(durationNanos));
    }

    private long elapsedNanos(long started) {
        return Math.max(0, nanoTime.getAsLong() - started);
    }

    private Counter counter(String suffix, String tagName, String tagValue) {
        return Counter.builder(METRIC_PREFIX + "." + suffix)
                .tag(tagName, tagValue)
                .register(meterRegistry);
    }

    enum StopReason {
        EXHAUSTED("exhausted"),
        MAXIMUM_BATCHES("maximum_batches"),
        MAXIMUM_DURATION("maximum_duration"),
        FAILURE("failure"),
        OVERLAP("overlap_skipped"),
        DISABLED("disabled");

        private final String metricValue;

        StopReason(String metricValue) {
            this.metricValue = metricValue;
        }

        String metricValue() {
            return metricValue;
        }
    }

    record CleanupRunResult(int completedBatches, int deletedRows, StopReason stopReason, long durationMillis) {
        static CleanupRunResult notRun(StopReason reason) {
            return new CleanupRunResult(0, 0, reason, 0);
        }
    }
}
