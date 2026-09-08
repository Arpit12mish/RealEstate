package com.brandPitara.sfs.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RequestLoggingMetrics {

    private static final List<String> FILE_CATEGORIES =
            List.of("request", "request_reliable", "error", "security", "audit", "app");

    private final MeterRegistry meterRegistry;

    @PostConstruct
    void bind() {
        Tags requestTag = Tags.of("category", "request");

        Gauge.builder("sfs.logging.queue.size", RequestLoggingTelemetry::queueSize)
                .tags(requestTag).register(meterRegistry);
        Gauge.builder("sfs.logging.queue.capacity", RequestLoggingTelemetry::queueCapacity)
                .tags(requestTag).register(meterRegistry);
        Gauge.builder("sfs.logging.queue.utilization", RequestLoggingTelemetry::queueUtilization)
                .tags(requestTag).register(meterRegistry);
        Gauge.builder("sfs.logging.queue.peak", RequestLoggingTelemetry::queuePeak)
                .tags(requestTag).register(meterRegistry);
        FunctionCounter.builder("sfs.logging.events.accepted", this,
                        ignored -> RequestLoggingTelemetry.acceptedCount())
                .tags(requestTag).register(meterRegistry);
        FunctionCounter.builder("sfs.logging.events.discarded", this,
                        ignored -> RequestLoggingTelemetry.discardedCount())
                .tags(requestTag).register(meterRegistry);

        for (String category : FILE_CATEGORIES) {
            Tags tags = Tags.of("category", category);
            FunctionCounter.builder("sfs.logging.appender.failures", this,
                            ignored -> RequestLoggingTelemetry.appenderFailures(category))
                    .tags(tags).register(meterRegistry);
            FunctionCounter.builder("sfs.logging.bytes.written", this,
                            ignored -> RequestLoggingTelemetry.writtenBytes(category))
                    .tags(tags).register(meterRegistry);
        }
    }
}
