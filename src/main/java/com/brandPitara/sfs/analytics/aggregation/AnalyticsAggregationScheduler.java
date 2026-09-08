package com.brandPitara.sfs.analytics.aggregation;

import com.brandPitara.sfs.analytics.config.AnalyticsIngestionProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nightly rollup of the prior day's raw events into analytics_daily_search and
 * analytics_daily_entity_metrics. Dashboard reads hit these aggregates, never the raw
 * partitioned table - see AnalyticsDashboardService. Idempotent upsert by (date, key),
 * so a re-run (manual backfill, retry after failure) is always safe - re-running twice
 * for the same date recomputes and overwrites the same rows, it does not add to them.
 *
 * A 1-5 minute lag on dashboard metrics is acceptable per the task's own guidance;
 * nothing here needs to be real-time.
 *
 * Every count(*) below is intentionally NOT count(DISTINCT event_id): idempotency is
 * enforced upstream, at the single write path (AnalyticsEventJdbcRepository's
 * ON CONFLICT (event_id, occurred_at) DO NOTHING, backed by V162's unique index), so a
 * retried client delivery never produces a second raw row in the first place. count(*)
 * over analytics_event is therefore already a distinct-event count - duplicating that
 * logic into every aggregation query (and every future one) would be redundant and
 * would only paper over a correctness bug that no longer exists upstream.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsAggregationScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final AnalyticsIngestionProperties properties;
    private final MeterRegistry meterRegistry;

    private Counter startedCounter;
    private Counter completedCounter;
    private Counter failedCounter;
    private Counter skippedCounter;
    private Timer durationTimer;

    /**
     * Guards against the scheduled nightly run and a manual backfill
     * (AnalyticsDashboardController#runAggregation, which executes on an HTTP worker
     * thread - a completely different thread from the single shared @Scheduled pool
     * thread) overlapping: two concurrent runFor() calls would both full-scan the same
     * partition and race harmlessly-but-wastefully on the same ON CONFLICT rows. Global
     * rather than per-date, matching the existing RefreshTokenCleanupScheduler
     * "overlap_skipped" pattern - a JVM-local lock, not distributed, since this
     * codebase's production is single-instance.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    void init() {
        startedCounter = Counter.builder("analytics.aggregation.started").register(meterRegistry);
        completedCounter = Counter.builder("analytics.aggregation.completed").register(meterRegistry);
        failedCounter = Counter.builder("analytics.aggregation.failed").register(meterRegistry);
        skippedCounter = Counter.builder("analytics.aggregation.skipped").register(meterRegistry);
        durationTimer = Timer.builder("analytics.aggregation.duration").register(meterRegistry);
    }

    /**
     * The scheduled entry point respects the ingestion kill switch (sfs.analytics.
     * ingestion.enabled) - if analytics is turned off, nightly aggregation of whatever
     * is already in the table is skipped too. runFor() itself stays unconditional so a
     * manual re-run (AnalyticsDashboardController#runAggregation) still works during an
     * incident even while the switch is off.
     */
    @Scheduled(cron = "${sfs.analytics.aggregation.schedule:0 30 2 * * *}")
    public void aggregateYesterday() {
        if (!properties.isEnabled()) {
            log.info("event=analytics_aggregation_skipped reason=disabled");
            return;
        }
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        runFor(yesterday);
    }

    /** Public so a manual backfill/retry (AnalyticsDashboardController#runAggregation) can target an arbitrary day. */
    public void runFor(LocalDate date) {
        if (!running.compareAndSet(false, true)) {
            skippedCounter.increment();
            log.info("event=analytics_aggregation_skipped reason=overlap date={}", date);
            return;
        }
        try {
            startedCounter.increment();
            long startNanos = System.nanoTime();
            Timestamp from = Timestamp.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
            Timestamp to = Timestamp.from(date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
            try {
                int searchRows = upsertSearchMetrics(from, to);
                int clickRows = updateSearchClickMetrics(from, to);
                int entityRows = upsertEntityMetrics(from, to);
                log.info("event=analytics_aggregation_completed date={} searchRows={} clickRows={} entityRows={}",
                        date, searchRows, clickRows, entityRows);
                completedCounter.increment();
            } catch (RuntimeException ex) {
                failedCounter.increment();
                log.warn("event=analytics_aggregation_failed date={}", date, ex);
            } finally {
                durationTimer.record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
            }
        } finally {
            running.set(false);
        }
    }

    private int upsertSearchMetrics(Timestamp from, Timestamp to) {
        String sql = """
                INSERT INTO analytics_daily_search (
                    metric_date, normalized_query, search_count, unique_user_count,
                    zero_result_count, avg_result_count, updated_at
                )
                SELECT
                    date(occurred_at AT TIME ZONE 'UTC') AS metric_date,
                    lower(trim(coalesce(properties ->> 'normalizedQuery', properties ->> 'query', ''))) AS normalized_query,
                    count(*) AS search_count,
                    count(DISTINCT coalesce(user_id::text, anonymous_id::text)) AS unique_user_count,
                    count(*) FILTER (WHERE (properties ->> 'resultCount')::int = 0) AS zero_result_count,
                    avg((properties ->> 'resultCount')::numeric) AS avg_result_count,
                    now()
                FROM analytics_event
                WHERE event_name = 'SEARCH_SUBMITTED'
                  AND occurred_at >= ? AND occurred_at < ?
                  AND coalesce(properties ->> 'normalizedQuery', properties ->> 'query') IS NOT NULL
                GROUP BY 1, 2
                ON CONFLICT (metric_date, normalized_query) DO UPDATE SET
                    search_count = EXCLUDED.search_count,
                    unique_user_count = EXCLUDED.unique_user_count,
                    zero_result_count = EXCLUDED.zero_result_count,
                    avg_result_count = EXCLUDED.avg_result_count,
                    updated_at = now()
                """;
        return jdbcTemplate.update(sql, from, to);
    }

    private int updateSearchClickMetrics(Timestamp from, Timestamp to) {
        String sql = """
                WITH submitted AS (
                    SELECT search_id,
                           date(occurred_at AT TIME ZONE 'UTC') AS metric_date,
                           lower(trim(coalesce(properties ->> 'normalizedQuery', properties ->> 'query', ''))) AS normalized_query
                    FROM analytics_event
                    WHERE event_name = 'SEARCH_SUBMITTED'
                      AND occurred_at >= ? AND occurred_at < ?
                      AND search_id IS NOT NULL
                ),
                click_stats AS (
                    SELECT s.metric_date, s.normalized_query,
                           count(*) AS clicks,
                           avg(c.result_position) AS avg_pos
                    FROM analytics_event c
                    JOIN submitted s ON s.search_id = c.search_id
                    WHERE c.event_name = 'SEARCH_RESULT_CLICKED'
                    GROUP BY 1, 2
                )
                UPDATE analytics_daily_search d
                SET click_count = click_stats.clicks,
                    avg_click_position = click_stats.avg_pos,
                    updated_at = now()
                FROM click_stats
                WHERE d.metric_date = click_stats.metric_date
                  AND d.normalized_query = click_stats.normalized_query
                """;
        return jdbcTemplate.update(sql, from, to);
    }

    private int upsertEntityMetrics(Timestamp from, Timestamp to) {
        String sql = """
                INSERT INTO analytics_daily_entity_metrics (
                    metric_date, entity_type, entity_id, view_count, click_count, unique_user_count, updated_at
                )
                SELECT
                    date(occurred_at AT TIME ZONE 'UTC') AS metric_date,
                    entity_type,
                    entity_id,
                    count(*) FILTER (WHERE event_name IN
                        ('PROJECT_VIEWED', 'BUILDER_VIEWED', 'ARTICLE_VIEWED', 'COMPANY_VIEWED',
                         'ARCHITECT_VIEWED', 'DESIGNER_VIEWED')) AS view_count,
                    count(*) FILTER (WHERE event_name = 'CARD_CLICKED') AS click_count,
                    count(DISTINCT coalesce(user_id::text, anonymous_id::text)) AS unique_user_count,
                    now()
                FROM analytics_event
                WHERE occurred_at >= ? AND occurred_at < ?
                  AND entity_type IS NOT NULL AND entity_id IS NOT NULL
                GROUP BY 1, 2, 3
                ON CONFLICT (metric_date, entity_type, entity_id) DO UPDATE SET
                    view_count = EXCLUDED.view_count,
                    click_count = EXCLUDED.click_count,
                    unique_user_count = EXCLUDED.unique_user_count,
                    updated_at = now()
                """;
        return jdbcTemplate.update(sql, from, to);
    }
}
