package com.brandPitara.sfs.analytics.service;

import com.brandPitara.sfs.analytics.config.AnalyticsIngestionProperties;
import com.brandPitara.sfs.analytics.dto.AnalyticsEventBatchRequest;
import com.brandPitara.sfs.analytics.dto.AnalyticsEventRequest;
import com.brandPitara.sfs.analytics.dto.AnalyticsIngestResponse;
import com.brandPitara.sfs.analytics.ingestion.AnalyticsEventRecord;
import com.brandPitara.sfs.analytics.ingestion.AnalyticsIngestionQueue;
import com.brandPitara.sfs.analytics.validation.AnalyticsEventValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AnalyticsIngestionService {

    private final AnalyticsEventValidator validator;
    private final AnalyticsIngestionQueue queue;
    private final AnalyticsIngestionProperties properties;
    private final MeterRegistry meterRegistry;

    private Counter rejectedCounter;

    @PostConstruct
    void init() {
        rejectedCounter = Counter.builder("analytics.events.rejected").register(meterRegistry);
    }

    /**
     * userId is resolved by the controller from the validated JWT / request attributes
     * that JwtRequestFilter already sets - never accepted from the client payload, so a
     * client cannot attribute events to a userId it doesn't hold a token for.
     *
     * When sfs.analytics.ingestion.enabled=false (the kill switch), this is a no-op:
     * every event is silently dropped and the response reports 0/0, never an error -
     * the caller must never see analytics being disabled as a failure.
     */
    public AnalyticsIngestResponse ingest(AnalyticsEventBatchRequest batch, Long userId) {
        if (!properties.isEnabled()) {
            return AnalyticsIngestResponse.builder().accepted(0).rejected(0).build();
        }

        Instant receivedAt = Instant.now();
        int accepted = 0;
        int rejected = 0;

        for (AnalyticsEventRequest event : batch.getEvents()) {
            if (!validator.isAcceptable(event)) {
                rejected++;
                rejectedCounter.increment();
                continue;
            }
            String propertiesJson = validator.normalizeProperties(event);
            AnalyticsEventRecord record = new AnalyticsEventRecord(
                    event.getEventId(),
                    event.getOccurredAt(),
                    receivedAt,
                    (short) event.getSchemaVersion(),
                    event.getEventName().toUpperCase(),
                    event.getAnonymousId(),
                    userId,
                    event.getSessionId(),
                    event.getScreen(),
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getSearchId(),
                    event.getPosition(),
                    event.getNumericValue(),
                    event.getSourcePlatform().toUpperCase(),
                    event.getAppVersion(),
                    propertiesJson
            );
            queue.enqueue(record);
            accepted++;
        }

        return AnalyticsIngestResponse.builder()
                .accepted(accepted)
                .rejected(rejected)
                .build();
    }
}
