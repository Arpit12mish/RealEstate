package com.brandPitara.sfs.analytics.validation;

import com.brandPitara.sfs.analytics.config.AnalyticsIngestionProperties;
import com.brandPitara.sfs.analytics.dto.AnalyticsEventRequest;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsEventValidatorTest {

    private AnalyticsEventValidator validator;
    private AnalyticsIngestionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AnalyticsIngestionProperties();
        validator = new AnalyticsEventValidator(properties, new ObjectMapper(), new LogSanitizer());
    }

    private AnalyticsEventRequest wellFormedEvent() {
        AnalyticsEventRequest event = new AnalyticsEventRequest();
        event.setEventName("SEARCH_SUBMITTED");
        event.setOccurredAt(Instant.now());
        event.setEventId(UUID.randomUUID());
        event.setAnonymousId(UUID.randomUUID());
        event.setSessionId(UUID.randomUUID());
        event.setSourcePlatform("ANDROID");
        return event;
    }

    @Test
    void acceptsWellFormedEvent() {
        assertThat(validator.isAcceptable(wellFormedEvent())).isTrue();
    }

    @Test
    void rejectsUnknownEventName() {
        AnalyticsEventRequest event = wellFormedEvent();
        event.setEventName("SOME_MADE_UP_EVENT");
        assertThat(validator.isAcceptable(event)).isFalse();
    }

    @Test
    void acceptsEventNameCaseInsensitively() {
        AnalyticsEventRequest event = wellFormedEvent();
        event.setEventName("search_submitted");
        assertThat(validator.isAcceptable(event)).isTrue();
    }

    @Test
    void rejectsUnknownSourcePlatform() {
        AnalyticsEventRequest event = wellFormedEvent();
        event.setSourcePlatform("WINDOWS_PHONE");
        assertThat(validator.isAcceptable(event)).isFalse();
    }

    @Test
    void rejectsMissingOccurredAt() {
        AnalyticsEventRequest event = wellFormedEvent();
        event.setOccurredAt(null);
        assertThat(validator.isAcceptable(event)).isFalse();
    }

    @Test
    void rejectsOccurredAtTooFarInTheFuture() {
        AnalyticsEventRequest event = wellFormedEvent();
        event.setOccurredAt(Instant.now().plusSeconds(2 * 86_400));
        assertThat(validator.isAcceptable(event)).isFalse();
    }

    @Test
    void acceptsOccurredAtJustInsideTheFutureTolerance() {
        // FUTURE_TOLERANCE_SECONDS is 24h - 23h ahead must still be accepted (ordinary
        // clock skew / timezone handling, not a bogus clock).
        AnalyticsEventRequest event = wellFormedEvent();
        event.setOccurredAt(Instant.now().plusSeconds(23 * 3600));
        assertThat(validator.isAcceptable(event)).isTrue();
    }

    @Test
    void rejectsOccurredAtJustOutsideTheFutureTolerance() {
        AnalyticsEventRequest event = wellFormedEvent();
        event.setOccurredAt(Instant.now().plusSeconds(25 * 3600));
        assertThat(validator.isAcceptable(event)).isFalse();
    }

    @Test
    void rejectsOccurredAtTooFarInThePast() {
        AnalyticsEventRequest event = wellFormedEvent();
        event.setOccurredAt(Instant.now().minusSeconds(400L * 86_400));
        assertThat(validator.isAcceptable(event)).isFalse();
    }

    @Test
    void acceptsOccurredAtWithinRetentionWindow() {
        // Default rawRetentionDays is 120 - well within it must still be accepted.
        AnalyticsEventRequest event = wellFormedEvent();
        event.setOccurredAt(Instant.now().minusSeconds(100L * 86_400));
        assertThat(validator.isAcceptable(event)).isTrue();
    }

    @Test
    void rejectsOccurredAtPastRetentionPlusGrace() {
        // Backend-optimization audit finding: AnalyticsPartitionMaintenanceScheduler
        // never backfills partitions for past months, and dropExpiredPartitions()
        // explicitly excludes analytics_event_default from cleanup - so an "old but
        // technically valid" timestamp beyond what retention actually keeps a named
        // partition for would land in the default partition forever, uncovered by
        // retention. The acceptance window must not outlive rawRetentionDays + grace.
        AnalyticsEventRequest event = wellFormedEvent();
        event.setOccurredAt(Instant.now().minusSeconds(130L * 86_400)); // 120 + 7 grace + 3
        assertThat(validator.isAcceptable(event)).isFalse();
    }

    @Test
    void pastAcceptanceWindowTracksConfiguredRetention() {
        properties.setRawRetentionDays(10);
        AnalyticsEventValidator shortRetentionValidator =
                new AnalyticsEventValidator(properties, new ObjectMapper(), new LogSanitizer());

        AnalyticsEventRequest event = wellFormedEvent();
        event.setOccurredAt(Instant.now().minusSeconds(20L * 86_400)); // well past 10 + 7 grace

        assertThat(shortRetentionValidator.isAcceptable(event)).isFalse();
    }

    @Test
    void normalizePropertiesReturnsNullWhenAbsent() {
        AnalyticsEventRequest event = wellFormedEvent();
        assertThat(validator.normalizeProperties(event)).isNull();
    }

    @Test
    void normalizePropertiesSerializesSmallMap() {
        AnalyticsEventRequest event = wellFormedEvent();
        event.setProperties(Map.of("query", "3 bhk noida", "resultCount", 12));
        String json = validator.normalizeProperties(event);
        assertThat(json).contains("3 bhk noida").contains("resultCount");
    }

    @Test
    void normalizePropertiesDropsOversizedMap() {
        properties.setMaxPropertiesJsonLength(64);
        AnalyticsEventRequest event = wellFormedEvent();
        event.setProperties(Map.of("query", "x".repeat(200)));
        assertThat(validator.normalizeProperties(event)).isNull();
    }

    @Test
    void normalizePropertiesMasksPhoneNumberInQueryField() {
        AnalyticsEventRequest event = wellFormedEvent();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("query", "call me on 9876543210 about this flat");
        event.setProperties(props);

        String json = validator.normalizeProperties(event);

        assertThat(json).doesNotContain("9876543210").contains("****");
    }

    @Test
    void normalizePropertiesMasksEmailInNormalizedQueryField() {
        AnalyticsEventRequest event = wellFormedEvent();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("normalizedQuery", "contact buyer at jane.doe@example.com please");
        event.setProperties(props);

        String json = validator.normalizeProperties(event);

        assertThat(json).doesNotContain("jane.doe@example.com").contains("****");
    }

    @Test
    void normalizePropertiesLeavesOrdinaryQueriesUnchanged() {
        AnalyticsEventRequest event = wellFormedEvent();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("query", "3 bhk noida");
        props.put("normalizedQuery", "3 bhk noida");
        event.setProperties(props);

        String json = validator.normalizeProperties(event);

        assertThat(json).contains("3 bhk noida").doesNotContain("****");
    }

    @Test
    void normalizePropertiesDoesNotMaskNonQueryFields() {
        // Masking is scoped to query/normalizedQuery only - resultCount and any other
        // supplementary field must pass through untouched.
        AnalyticsEventRequest event = wellFormedEvent();
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("query", "3 bhk noida");
        props.put("resultCount", 12);
        props.put("sourceSearchId", "9876543210-not-actually-a-phone-but-looks-like-one");
        event.setProperties(props);

        String json = validator.normalizeProperties(event);

        assertThat(json).contains("\"resultCount\":12").contains("9876543210-not-actually-a-phone-but-looks-like-one");
    }
}
