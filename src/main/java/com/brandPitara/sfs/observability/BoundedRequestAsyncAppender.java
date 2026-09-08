package com.brandPitara.sfs.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * One-delegate, bounded asynchronous appender for low-priority request INFO
 * events. Queue insertion is always non-blocking; WARN/ERROR events have a
 * synchronous safety fallback and are never counted as discarded.
 */
public class BoundedRequestAsyncAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
        implements AppenderAttachable<ILoggingEvent> {

    private final AppenderAttachableImpl<ILoggingEvent> appenders = new AppenderAttachableImpl<>();

    private int queueSize = 2_048;
    private int discardingThreshold = 0;
    private boolean neverBlock = true;
    private boolean includeCallerData = false;
    private int maxFlushTime = 5_000;

    private BlockingQueue<ILoggingEvent> queue;
    private Worker worker;
    private volatile boolean shutdownRequested;
    private volatile boolean abandonQueue;

    @Override
    public void start() {
        if (queueSize < 1) {
            addError("queueSize must be greater than zero");
            return;
        }
        if (discardingThreshold < 0 || discardingThreshold >= queueSize) {
            addError("discardingThreshold must be between zero and queueSize - 1");
            return;
        }
        if (!neverBlock) {
            addError("Bounded request logging requires neverBlock=true");
            return;
        }
        if (!appenders.iteratorForAppenders().hasNext()) {
            addError("One delegate appender is required");
            return;
        }

        queue = new ArrayBlockingQueue<>(queueSize);
        shutdownRequested = false;
        abandonQueue = false;
        worker = new Worker();
        worker.setDaemon(true);
        worker.setName("sfs-request-log-async");
        super.start();
        RequestLoggingTelemetry.register(this);
        worker.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        event.prepareForDeferredProcessing();
        if (includeCallerData) event.getCallerData();

        if (isDiscardable(event) && queue.remainingCapacity() < discardingThreshold) {
            RequestLoggingTelemetry.discarded(1);
            return;
        }

        if (queue.offer(event)) {
            RequestLoggingTelemetry.accepted(queue.size());
            return;
        }

        if (isDiscardable(event)) {
            RequestLoggingTelemetry.discarded(1);
        } else {
            // Defensive only: production routes WARN/ERROR to a separate logger.
            appenders.appendLoopOnAppenders(event);
        }
    }

    @Override
    public void stop() {
        if (!isStarted()) return;

        super.stop();
        shutdownRequested = true;
        worker.interrupt();

        boolean interrupted = false;
        try {
            worker.join(maxFlushTime);
        } catch (InterruptedException ex) {
            interrupted = true;
        }

        if (worker.isAlive()) {
            abandonQueue = true;
            int abandoned = queue.size();
            queue.clear();
            RequestLoggingTelemetry.discarded(abandoned);
            addWarn("Request-log shutdown flush exceeded " + maxFlushTime
                    + "ms; abandoned " + abandoned + " queued events");
            worker.interrupt();
        } else {
            appenders.detachAndStopAllAppenders();
        }

        RequestLoggingTelemetry.unregister(this);
        if (interrupted) Thread.currentThread().interrupt();
    }

    public int getNumberOfElementsInQueue() {
        return queue == null ? 0 : queue.size();
    }

    public int getRemainingCapacity() {
        return queue == null ? queueSize : queue.remainingCapacity();
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getDiscardingThreshold() {
        return discardingThreshold;
    }

    public void setDiscardingThreshold(int discardingThreshold) {
        this.discardingThreshold = discardingThreshold;
    }

    public boolean isNeverBlock() {
        return neverBlock;
    }

    public void setNeverBlock(boolean neverBlock) {
        this.neverBlock = neverBlock;
    }

    public boolean isIncludeCallerData() {
        return includeCallerData;
    }

    public void setIncludeCallerData(boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

    public int getMaxFlushTime() {
        return maxFlushTime;
    }

    public void setMaxFlushTime(int maxFlushTime) {
        this.maxFlushTime = maxFlushTime;
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        if (appenders.iteratorForAppenders().hasNext()) {
            addWarn("Only one delegate appender is supported; ignoring " + newAppender.getName());
            return;
        }
        appenders.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return appenders.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return appenders.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return appenders.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenders.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return appenders.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return appenders.detachAppender(name);
    }

    private boolean isDiscardable(ILoggingEvent event) {
        return event.getLevel().toInt() <= Level.INFO_INT;
    }

    private final class Worker extends Thread {
        @Override
        public void run() {
            try {
                while ((!shutdownRequested || !queue.isEmpty()) && !abandonQueue) {
                    try {
                        ILoggingEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (event != null) appenders.appendLoopOnAppenders(event);
                    } catch (InterruptedException ignored) {
                        // Re-check shutdown/abandon flags and continue draining when allowed.
                    } catch (RuntimeException ex) {
                        RequestLoggingTelemetry.appenderFailure("request");
                        addError("Request-log delegate failed", ex);
                    }
                }
            } finally {
                if (abandonQueue) appenders.detachAndStopAllAppenders();
            }
        }
    }
}
