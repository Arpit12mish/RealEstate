package com.brandPitara.sfs.analytics.ingestion;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-enriched, fully-validated event ready for the queue and batch insert.
 * Built once per accepted client event by AnalyticsIngestionService - userId is the
 * server-resolved identity (from the validated JWT / request attributes), never the
 * client payload.
 */
public record AnalyticsEventRecord(
        UUID eventId,
        Instant occurredAt,
        Instant receivedAt,
        short schemaVersion,
        String eventName,
        UUID anonymousId,
        Long userId,
        UUID sessionId,
        String screen,
        String entityType,
        Long entityId,
        UUID searchId,
        Integer position,
        Double numericValue,
        String sourcePlatform,
        String appVersion,
        String propertiesJson
) {
}
