package com.brandPitara.sfs.analytics.validation;

import com.brandPitara.sfs.analytics.config.AnalyticsIngestionProperties;
import com.brandPitara.sfs.analytics.domain.AnalyticsEventName;
import com.brandPitara.sfs.analytics.domain.AnalyticsSourcePlatform;
import com.brandPitara.sfs.analytics.dto.AnalyticsEventRequest;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-event acceptance rules. An individual event failing validation is dropped (not
 * persisted) without failing the rest of the batch or returning an error to the client -
 * analytics must never surface as a client-visible failure (task Step 20).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventValidator {

    private static final Set<String> EVENT_NAMES = namesOf(AnalyticsEventName.values());
    private static final Set<String> PLATFORMS = namesOf(AnalyticsSourcePlatform.values());

    /**
     * Extra slack beyond raw retention before an old timestamp is rejected outright -
     * covers clock skew and a client that's been offline briefly, not a real backfill
     * window.
     */
    private static final long PAST_GRACE_DAYS = 7;

    /**
     * How far ahead of the server's clock a client-reported occurredAt may be before
     * it's treated as a bogus clock rather than ordinary skew. 24h comfortably covers
     * timezone/DST mishandling and modest device-clock drift, while staying far inside
     * AnalyticsPartitionMaintenanceScheduler's partition-lookahead window (3 months by
     * default) - so a future timestamp that passes this check can never miss a
     * pre-created partition and fall through to the default one.
     */
    private static final long FUTURE_TOLERANCE_SECONDS = 86_400;

    /** properties keys whose String value gets PII-masked before storage - see maskSearchQueryPii. */
    private static final Set<String> QUERY_BEARING_PROPERTY_KEYS = Set.of("query", "normalizedQuery");

    private final AnalyticsIngestionProperties properties;
    private final ObjectMapper objectMapper;
    private final LogSanitizer logSanitizer;

    public boolean isAcceptable(AnalyticsEventRequest event) {
        if (event.getEventName() == null || !EVENT_NAMES.contains(event.getEventName().toUpperCase())) {
            return false;
        }
        if (event.getSourcePlatform() == null || !PLATFORMS.contains(event.getSourcePlatform().toUpperCase())) {
            return false;
        }
        if (event.getOccurredAt() == null) {
            return false;
        }
        // Reject obviously bogus client clocks rather than letting them corrupt a
        // partition's date range: at most FUTURE_TOLERANCE_SECONDS ahead, and no further
        // into the past than retention actually keeps data for. AnalyticsPartitionMaintenance-
        // Scheduler only ever pre-creates partitions for the current month forward, and
        // dropExpiredPartitions() never touches analytics_event_default - so a wider past
        // window here would let old-but-"valid" timestamps land permanently in the
        // default partition, never covered by retention. Tied to the same
        // rawRetentionDays config so the two stay consistent if retention changes.
        Instant now = Instant.now();
        long maxPastSeconds = (properties.getRawRetentionDays() + PAST_GRACE_DAYS) * 86_400L;
        if (event.getOccurredAt().isAfter(now.plusSeconds(FUTURE_TOLERANCE_SECONDS))
                || event.getOccurredAt().isBefore(now.minusSeconds(maxPastSeconds))) {
            return false;
        }
        return true;
    }

    /**
     * Serializes and caps the properties map. Oversized properties are dropped (the
     * event itself still persists without them) rather than rejecting the whole event -
     * properties is supplementary, not load-bearing.
     * <p>
     * Search-query PII hardening: "query"/"normalizedQuery" values are masked via the
     * same LogSanitizer#sanitizeMessage already trusted for production log safety
     * (phone numbers, emails, tokens/JWTs, sensitive key=value pairs) before anything
     * is serialized or length-checked - a user who pastes their phone number or email
     * into search must not have it land verbatim in analytics_event.properties. Masking
     * runs first, on the already length-bounded per-request payload (see
     * AnalyticsIngestionController's request-size cap), so this can't become a CPU cost
     * vector, and the regexes involved are single linear-pass patterns with no
     * backtracking risk (see LogSanitizer).
     */
    public String normalizeProperties(AnalyticsEventRequest event) {
        if (event.getProperties() == null || event.getProperties().isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(maskSearchQueryPii(event.getProperties()));
            if (json.length() > properties.getMaxPropertiesJsonLength()) {
                log.debug("event=analytics_properties_truncated eventName={} length={}",
                        event.getEventName(), json.length());
                return null;
            }
            return json;
        } catch (Exception ex) {
            log.debug("event=analytics_properties_encode_failed eventName={}", event.getEventName());
            return null;
        }
    }

    private Map<String, Object> maskSearchQueryPii(Map<String, Object> source) {
        Map<String, Object> masked = new HashMap<>(source);
        for (String key : QUERY_BEARING_PROPERTY_KEYS) {
            if (masked.get(key) instanceof String text) {
                masked.put(key, logSanitizer.sanitizeMessage(text));
            }
        }
        return masked;
    }

    private static Set<String> namesOf(Enum<?>[] values) {
        Set<String> names = new HashSet<>();
        Arrays.stream(values).forEach(v -> names.add(v.name()));
        return names;
    }
}
