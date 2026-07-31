package com.brandPitara.sfs.observability;

import ch.qos.logback.core.rolling.RollingFileAppender;

import java.io.IOException;

/** Rolling file appender with aggregate byte/failure counters only. */
public class InstrumentedRollingFileAppender<E> extends RollingFileAppender<E> {

    private String metricCategory = "unknown";

    public void setMetricCategory(String metricCategory) {
        this.metricCategory = metricCategory;
    }

    @Override
    public void start() {
        super.start();
        if (!isStarted()) RequestLoggingTelemetry.appenderFailure(metricCategory);
    }

    @Override
    protected void updateByteCount(byte[] byteArray) {
        super.updateByteCount(byteArray);
        RequestLoggingTelemetry.writtenBytes(metricCategory, byteArray.length);
    }

    @Override
    protected void writeOut(E event) throws IOException {
        try {
            super.writeOut(event);
        } catch (IOException | RuntimeException ex) {
            RequestLoggingTelemetry.appenderFailure(metricCategory);
            throw ex;
        }
    }
}
