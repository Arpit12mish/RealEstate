package com.brandPitara.sfs.observability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

final class RequestLoggingTelemetry {

    private static final LongAdder DISCARDED = new LongAdder();
    private static final LongAdder ACCEPTED = new LongAdder();
    private static final AtomicInteger QUEUE_PEAK = new AtomicInteger();
    private static final Map<String, LongAdder> APPENDER_FAILURES = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> WRITTEN_BYTES = new ConcurrentHashMap<>();

    private static volatile BoundedRequestAsyncAppender asyncAppender;

    private RequestLoggingTelemetry() {
    }

    static void register(BoundedRequestAsyncAppender appender) {
        asyncAppender = appender;
    }

    static void unregister(BoundedRequestAsyncAppender appender) {
        if (asyncAppender == appender) asyncAppender = null;
    }

    static void accepted(int queueDepth) {
        ACCEPTED.increment();
        QUEUE_PEAK.accumulateAndGet(queueDepth, Math::max);
    }

    static void discarded(long count) {
        if (count > 0) DISCARDED.add(count);
    }

    static void appenderFailure(String category) {
        APPENDER_FAILURES.computeIfAbsent(safeCategory(category), ignored -> new LongAdder()).increment();
    }

    static void writtenBytes(String category, long bytes) {
        if (bytes > 0) {
            WRITTEN_BYTES.computeIfAbsent(safeCategory(category), ignored -> new LongAdder()).add(bytes);
        }
    }

    static long discardedCount() {
        return DISCARDED.sum();
    }

    static long acceptedCount() {
        return ACCEPTED.sum();
    }

    static int queueSize() {
        BoundedRequestAsyncAppender appender = asyncAppender;
        return appender != null && appender.isStarted() ? appender.getNumberOfElementsInQueue() : 0;
    }

    static int queueCapacity() {
        BoundedRequestAsyncAppender appender = asyncAppender;
        return appender != null ? appender.getQueueSize() : 0;
    }

    static double queueUtilization() {
        int capacity = queueCapacity();
        return capacity == 0 ? 0 : (double) queueSize() / capacity;
    }

    static int queuePeak() {
        return QUEUE_PEAK.get();
    }

    static long appenderFailures(String category) {
        LongAdder value = APPENDER_FAILURES.get(safeCategory(category));
        return value == null ? 0 : value.sum();
    }

    static long writtenBytes(String category) {
        LongAdder value = WRITTEN_BYTES.get(safeCategory(category));
        return value == null ? 0 : value.sum();
    }

    static void resetForTests() {
        DISCARDED.reset();
        ACCEPTED.reset();
        QUEUE_PEAK.set(0);
        APPENDER_FAILURES.clear();
        WRITTEN_BYTES.clear();
        asyncAppender = null;
    }

    private static String safeCategory(String category) {
        return category == null || category.isBlank() ? "unknown" : category;
    }
}
