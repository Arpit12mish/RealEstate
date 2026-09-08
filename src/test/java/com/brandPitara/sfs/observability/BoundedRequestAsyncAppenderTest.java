package com.brandPitara.sfs.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class BoundedRequestAsyncAppenderTest {

    private final LoggerContext context = isolatedContext();
    private final List<BoundedRequestAsyncAppender> appenders = new CopyOnWriteArrayList<>();

    @BeforeEach
    void resetTelemetry() {
        RequestLoggingTelemetry.resetForTests();
    }

    @AfterEach
    void stopAppenders() {
        appenders.forEach(BoundedRequestAsyncAppender::stop);
        context.stop();
        RequestLoggingTelemetry.resetForTests();
    }

    @Test
    void queueIsBoundedAndFullInfoEventsAreDiscardedWithoutBlocking() throws Exception {
        BlockingAppender delegate = new BlockingAppender();
        delegate.setContext(context);
        delegate.start();
        BoundedRequestAsyncAppender async = async(delegate, 2, 0, 1_000);

        async.doAppend(event(Level.INFO, "worker"));
        assertThat(delegate.entered.await(1, TimeUnit.SECONDS)).isTrue();

        async.doAppend(event(Level.INFO, "queued-1"));
        async.doAppend(event(Level.INFO, "queued-2"));
        long started = System.nanoTime();
        async.doAppend(event(Level.INFO, "discarded"));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(async.getNumberOfElementsInQueue()).isEqualTo(2);
        assertThat(RequestLoggingTelemetry.queuePeak()).isEqualTo(2);
        assertThat(RequestLoggingTelemetry.discardedCount()).isEqualTo(1);
        assertThat(elapsedMillis).isLessThan(100);
        delegate.release.countDown();
    }

    @Test
    void discardThresholdDropsInfoBeforeQueueIsCompletelyFull() throws Exception {
        BlockingAppender delegate = new BlockingAppender();
        delegate.setContext(context);
        delegate.start();
        BoundedRequestAsyncAppender async = async(delegate, 4, 2, 1_000);

        async.doAppend(event(Level.INFO, "worker"));
        assertThat(delegate.entered.await(1, TimeUnit.SECONDS)).isTrue();
        async.doAppend(event(Level.INFO, "queued-1"));
        async.doAppend(event(Level.INFO, "queued-2"));
        async.doAppend(event(Level.INFO, "queued-3"));
        async.doAppend(event(Level.INFO, "threshold-discard"));

        assertThat(async.getNumberOfElementsInQueue()).isEqualTo(3);
        assertThat(RequestLoggingTelemetry.discardedCount()).isEqualTo(1);
        delegate.release.countDown();
    }

    @Test
    void warnAndErrorUseSynchronousFallbackWhenQueueIsFull() throws Exception {
        FirstEventBlockingAppender delegate = new FirstEventBlockingAppender();
        delegate.setContext(context);
        delegate.start();
        BoundedRequestAsyncAppender async = async(delegate, 1, 0, 1_000);

        async.doAppend(event(Level.INFO, "worker"));
        assertThat(delegate.entered.await(1, TimeUnit.SECONDS)).isTrue();
        async.doAppend(event(Level.INFO, "queued"));
        async.doAppend(event(Level.WARN, "preserved-warning"));
        async.doAppend(event(Level.ERROR, "preserved-error"));

        assertThat(delegate.messages).contains("preserved-warning", "preserved-error");
        assertThat(RequestLoggingTelemetry.discardedCount()).isZero();
        delegate.release.countDown();
    }

    @Test
    void discardedEventAndQueueGaugesArePublishedWithoutHighCardinalityTags() throws Exception {
        BlockingAppender delegate = new BlockingAppender();
        delegate.setContext(context);
        delegate.start();
        BoundedRequestAsyncAppender async = async(delegate, 1, 0, 1_000);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new RequestLoggingMetrics(registry).bind();

        async.doAppend(event(Level.INFO, "worker"));
        assertThat(delegate.entered.await(1, TimeUnit.SECONDS)).isTrue();
        async.doAppend(event(Level.INFO, "queued"));
        async.doAppend(event(Level.INFO, "discarded"));

        assertThat(registry.get("sfs.logging.events.discarded").tag("category", "request").functionCounter().count())
                .isEqualTo(1);
        assertThat(registry.get("sfs.logging.queue.capacity").tag("category", "request").gauge().value())
                .isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().getTags()).noneMatch(tag -> tag.getKey().toLowerCase().contains("user")));
        delegate.release.countDown();
        registry.close();
    }

    @Test
    void shutdownFlushIsTimeBoundedAndUnflushedInfoIsCountedAsDiscarded() throws Exception {
        BlockingAppender delegate = new BlockingAppender();
        delegate.setContext(context);
        delegate.start();
        BoundedRequestAsyncAppender async = async(delegate, 3, 0, 50);

        async.doAppend(event(Level.INFO, "worker"));
        assertThat(delegate.entered.await(1, TimeUnit.SECONDS)).isTrue();
        async.doAppend(event(Level.INFO, "queued-1"));
        async.doAppend(event(Level.INFO, "queued-2"));

        long started = System.nanoTime();
        async.stop();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(elapsedMillis).isLessThan(500);
        assertThat(RequestLoggingTelemetry.discardedCount()).isEqualTo(2);
        delegate.release.countDown();
    }

    @Test
    void invalidBlockingPolicyCannotStart() {
        CollectingAppender delegate = new CollectingAppender();
        delegate.setContext(context);
        delegate.start();
        BoundedRequestAsyncAppender async = new BoundedRequestAsyncAppender();
        appenders.add(async);
        async.setContext(context);
        async.setNeverBlock(false);
        async.addAppender(delegate);
        async.start();

        assertThat(async.isStarted()).isFalse();
    }

    @Test
    void delegateFailureIncrementsAggregateFailureMetric() throws Exception {
        @SuppressWarnings("unchecked")
        Appender<ILoggingEvent> delegate = mock(Appender.class);
        doThrow(new IllegalStateException("simulated appender failure")).when(delegate).doAppend(any());
        BoundedRequestAsyncAppender async = async(delegate, 4, 0, 1_000);

        async.doAppend(event(Level.INFO, "fails"));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (RequestLoggingTelemetry.appenderFailures("request") == 0 && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }

        assertThat(RequestLoggingTelemetry.appenderFailures("request")).isEqualTo(1);
    }

    private BoundedRequestAsyncAppender async(
            Appender<ILoggingEvent> delegate,
            int queueSize,
            int threshold,
            int flushMillis
    ) {
        BoundedRequestAsyncAppender async = new BoundedRequestAsyncAppender();
        appenders.add(async);
        async.setContext(context);
        async.setName("TEST_ASYNC");
        async.setQueueSize(queueSize);
        async.setDiscardingThreshold(threshold);
        async.setNeverBlock(true);
        async.setIncludeCallerData(false);
        async.setMaxFlushTime(flushMillis);
        async.addAppender(delegate);
        async.start();
        assertThat(async.isStarted()).isTrue();
        return async;
    }

    private ILoggingEvent event(Level level, String message) {
        return new LoggingEvent(getClass().getName(), context.getLogger("test"), level, message, null, null);
    }

    private static LoggerContext isolatedContext() {
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        context.start();
        return context;
    }

    private static class BlockingAppender extends AppenderBase<ILoggingEvent> {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        protected void append(ILoggingEvent event) {
            entered.countDown();
            boolean interrupted = false;
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static final class FirstEventBlockingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final List<String> messages = new CopyOnWriteArrayList<>();
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        protected void append(ILoggingEvent event) {
            messages.add(event.getFormattedMessage());
            if (calls.getAndIncrement() != 0) return;
            entered.countDown();
            boolean interrupted = false;
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static final class CollectingAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
        }
    }

}
