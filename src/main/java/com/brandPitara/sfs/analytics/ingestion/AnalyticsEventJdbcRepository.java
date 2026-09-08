package com.brandPitara.sfs.analytics.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

/**
 * Bulk-insert-only repository for analytics_event. Deliberately not a JPA entity/
 * repository - a per-row EntityManager.persist() would defeat the point of batching at
 * this volume. batchUpdate issues one multi-statement round trip per drain cycle.
 *
 * properties is written via a "?::jsonb" text cast rather than the driver's PGobject -
 * the postgresql driver is a runtime-scoped dependency here (not on the compile
 * classpath), and a plain string cast avoids depending on driver-specific types.
 *
 * ON CONFLICT (event_id, occurred_at) DO NOTHING makes a retried delivery of the same
 * event a no-op instead of a duplicate row - see V162's migration comment for why this
 * pair is the correct idempotency key. insertBatch reports the ACTUAL number of rows
 * written (summing each row's JDBC batch result: 1 = inserted, 0 = skipped as a
 * duplicate), not just the batch size, so analytics.batch.inserted stays accurate even
 * when duplicates are silently absorbed here.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventJdbcRepository {

    private static final String INSERT_SQL = """
            INSERT INTO analytics_event (
                event_id, occurred_at, received_at, schema_version, event_name,
                anonymous_id, user_id, session_id, screen, entity_type, entity_id,
                search_id, result_position, numeric_value, source_platform, app_version, properties
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (event_id, occurred_at) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public int insertBatch(List<AnalyticsEventRecord> records) {
        if (records.isEmpty()) {
            return 0;
        }
        int[][] results = jdbcTemplate.batchUpdate(INSERT_SQL, records, records.size(), (ps, record) -> {
            ps.setObject(1, record.eventId());
            ps.setTimestamp(2, Timestamp.from(record.occurredAt()));
            ps.setTimestamp(3, Timestamp.from(record.receivedAt()));
            ps.setShort(4, record.schemaVersion());
            ps.setString(5, record.eventName());
            ps.setObject(6, record.anonymousId());
            if (record.userId() != null) {
                ps.setLong(7, record.userId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            ps.setObject(8, record.sessionId());
            ps.setString(9, record.screen());
            ps.setString(10, record.entityType());
            if (record.entityId() != null) {
                ps.setLong(11, record.entityId());
            } else {
                ps.setNull(11, Types.BIGINT);
            }
            ps.setObject(12, record.searchId());
            if (record.position() != null) {
                ps.setInt(13, record.position());
            } else {
                ps.setNull(13, Types.INTEGER);
            }
            if (record.numericValue() != null) {
                ps.setDouble(14, record.numericValue());
            } else {
                ps.setNull(14, Types.DOUBLE);
            }
            ps.setString(15, record.sourcePlatform());
            ps.setString(16, record.appVersion());
            if (record.propertiesJson() != null) {
                ps.setString(17, record.propertiesJson());
            } else {
                ps.setNull(17, Types.VARCHAR);
            }
        });
        return countInserted(results, records.size());
    }

    /**
     * Sums per-row JDBC batch results: 1 = inserted, 0 = skipped by ON CONFLICT DO
     * NOTHING. Some drivers report Statement.SUCCESS_NO_INFO (-2) instead of a precise
     * per-row count in batch mode; if that appears anywhere in the result, per-row
     * accounting isn't possible, so fall back to the full batch size rather than
     * underreport.
     */
    private int countInserted(int[][] results, int batchSize) {
        int inserted = 0;
        for (int[] chunk : results) {
            for (int rowResult : chunk) {
                if (rowResult == java.sql.Statement.SUCCESS_NO_INFO) {
                    return batchSize;
                }
                if (rowResult > 0) {
                    inserted += rowResult;
                }
            }
        }
        return inserted;
    }
}
