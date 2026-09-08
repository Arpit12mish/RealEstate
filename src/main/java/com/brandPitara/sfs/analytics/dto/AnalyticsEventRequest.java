package com.brandPitara.sfs.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One client-emitted event, as sent inside an {@link AnalyticsEventBatchRequest}.
 * `userId` is deliberately absent - the server derives it from the validated JWT
 * (see AnalyticsIngestionController), never trusting a client-supplied value, matching
 * how RateLimitIdentityResolver already treats identity for this codebase.
 */
@Getter
@Setter
public class AnalyticsEventRequest {

    @NotBlank
    @Size(max = 60)
    private String eventName;

    @NotNull
    private Instant occurredAt;

    @NotNull
    private UUID eventId;

    @NotNull
    private UUID anonymousId;

    @NotNull
    private UUID sessionId;

    @jakarta.validation.constraints.Min(1)
    private int schemaVersion = 1;

    @NotBlank
    @Size(max = 10)
    private String sourcePlatform;

    @Size(max = 30)
    private String appVersion;

    @Size(max = 80)
    private String screen;

    @Size(max = 40)
    private String entityType;

    private Long entityId;

    private UUID searchId;

    private Integer position;

    private Double numericValue;

    /** Bounded and validated at the service layer (AnalyticsEventValidator), not here - the
     * serialized size cap depends on config (sfs.analytics.ingestion.max-properties-json-length). */
    private Map<String, Object> properties;
}
