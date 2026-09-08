package com.brandPitara.sfs.analytics.ingestion;

import com.brandPitara.sfs.analytics.config.AnalyticsIngestionProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-process bounded queue + async drain worker for analytics events, mirroring the
 * shape this codebase already trusts in production for request logging
 * (observability.BoundedRequestAsyncAppender): enqueue is always non-blocking, and a
 * single daemon thread drains in batches and writes via JdbcTemplate.batchUpdate.
 *
 * This deliberately does NOT introduce SQS. There is no existing async/queue precedent
 * beyond the logging appender, current AWS footprint is a single EC2 instance with
 * self-managed Postgres, and analytics writes are explicitly non-critical - the
 * accepted tradeoff is that events buffered but not yet flushed are lost on JVM
 * restart/deploy. Revisit only if sustained write QPS or a durability requirement
 * outgrows this (see analytics architecture audit, Step 11/26).
 *
 * Metrics (production-hardening pass): analytics.events.{accepted,dropped},
 * analytics.queue.{depth,capacity} (gauges, read directly from the in-memory queue -
 * never polls the DB), analytics.batch.{inserted,failed,duration}. accepted/dropped
 * AtomicLongs are kept alongside the Micrometer counters (not replaced) - existing
 * tests and any direct programmatic use of getAcceptedCount()/getDroppedCount() still
 * work unchanged.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsIngestionQueue {

    private final AnalyticsIngestionProperties properties;
    private final AnalyticsEventJdbcRepository repository;
    private final MeterRegistry meterRegistry;

    private BlockingQueue<AnalyticsEventRecord> queue;
    private Thread worker;
    private volatile boolean shutdownRequested;

    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong persisted = new AtomicLong();
    private final AtomicLong lastDroppedLogAt = new AtomicLong();
    private final AtomicLong failedBatches = new AtomicLong();
    private final AtomicLong lastFailureLogAt = new AtomicLong();

    private Counter acceptedCounter;
    private Counter droppedCounter;
    private Counter insertedCounter;
    private Counter failedCounter;
    private Timer batchDuration;

    @PostConstruct
    void start() {
        queue = new ArrayBlockingQueue<>(properties.getQueueCapacity());
        shutdownRequested = false;

        acceptedCounter = Counter.builder("analytics.events.accepted").register(meterRegistry);
        droppedCounter = Counter.builder("analytics.events.dropped").register(meterRegistry);
        insertedCounter = Counter.builder("analytics.batch.inserted").register(meterRegistry);
        failedCounter = Counter.builder("analytics.batch.failed").register(meterRegistry);
        batchDuration = Timer.builder("analytics.batch.duration").register(meterRegistry);
        meterRegistry.gauge("analytics.queue.depth", this, AnalyticsIngestionQueue::getQueueSize);
        meterRegistry.gauge("analytics.queue.capacity", properties, AnalyticsIngestionProperties::getQueueCapacity);

        worker = new Thread(this::drainLoop, "sfs-analytics-ingest");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    void stop() {
        shutdownRequested = true;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(5_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Never blocks the calling request thread. Drops the event if the queue is full. */
    public void enqueue(AnalyticsEventRecord record) {
        accepted.incrementAndGet();
        acceptedCounter.increment();
        if (!queue.offer(record)) {
            long total = dropped.incrementAndGet();
            droppedCounter.increment();
            long now = System.currentTimeMillis();
            long last = lastDroppedLogAt.get();
            if (now - last > 60_000 && lastDroppedLogAt.compareAndSet(last, now)) {
                log.warn("event=analytics_queue_full totalDropped={}", total);
            }
        }
    }

    public int getQueueSize() {
        return queue == null ? 0 : queue.size();
    }

    public long getAcceptedCount() {
        return accepted.get();
    }

    public long getDroppedCount() {
        return dropped.get();
    }

    public long getPersistedCount() {
        return persisted.get();
    }

    public long getFailedBatchCount() {
        return failedBatches.get();
    }

    private void drainLoop() {
        List<AnalyticsEventRecord> buffer = new ArrayList<>(properties.getDrainBatchSize());
        while (!shutdownRequested || !queue.isEmpty()) {
            try {
                drainOnce(buffer);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException ex) {
                onDrainFailure(buffer.size(), ex);
                buffer.clear();
            }
        }
    }

    private void drainOnce(List<AnalyticsEventRecord> buffer) throws InterruptedException {
        buffer.clear();
        AnalyticsEventRecord first = queue.poll(properties.getDrainIntervalMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (first == null) {
            return;
        }
        buffer.add(first);
        queue.drainTo(buffer, properties.getDrainBatchSize() - 1);
        List<AnalyticsEventRecord> toInsert = buffer;
        int written = batchDuration.record(() -> repository.insertBatch(toInsert));
        persisted.addAndGet(written);
        insertedCounter.increment(written);
    }

    /**
     * Worker deliberately does not die and does not retry the failed batch (see task
     * "Retry / Idempotency" audit, Step B5): a batch that fails here is dropped, not
     * requeued - analytics is explicitly non-critical, and blind infinite retry of a
     * batch against a possibly-still-broken DB would risk an unbounded backlog. The
     * warning log is rate-limited the same way queue-full is, so a sustained DB outage
     * (one failure per drain interval) can't flood production logs; only the batch
     * contents are ever omitted from the log line, never included, since events may
     * carry a user's search text.
     */
    private void onDrainFailure(int batchSize, RuntimeException ex) {
        long total = failedBatches.incrementAndGet();
        failedCounter.increment();
        long now = System.currentTimeMillis();
        long last = lastFailureLogAt.get();
        if (now - last > 60_000 && lastFailureLogAt.compareAndSet(last, now)) {
            log.warn("event=analytics_drain_failed batchSize={} totalFailedBatches={}", batchSize, total, ex);
        }
    }
}
