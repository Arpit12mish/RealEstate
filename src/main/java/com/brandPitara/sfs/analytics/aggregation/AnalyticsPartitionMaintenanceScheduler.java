package com.brandPitara.sfs.analytics.aggregation;

import com.brandPitara.sfs.analytics.config.AnalyticsIngestionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Keeps analytics_event partitioned ahead of need and drops raw partitions past
 * retention. This codebase had no existing partitioning precedent before this module,
 * so unlike the rest of the app there's no established maintenance job to follow - a
 * DEFAULT partition (created in V161) is the safety net if this job ever falls behind:
 * inserts still succeed, they just land somewhere index-unfriendly until the next run
 * catches up.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsPartitionMaintenanceScheduler {

    private static final DateTimeFormatter PARTITION_SUFFIX = DateTimeFormatter.ofPattern("yyyy_MM");

    private final JdbcTemplate jdbcTemplate;
    private final AnalyticsIngestionProperties properties;

    @Scheduled(cron = "${sfs.analytics.partition-maintenance.schedule:0 0 3 * * *}")
    public void maintain() {
        createUpcomingPartitions();
        dropExpiredPartitions();
    }

    private void createUpcomingPartitions() {
        LocalDate startMonth = LocalDate.now().withDayOfMonth(1);
        for (int i = 0; i <= properties.getPartitionLookaheadMonths(); i++) {
            LocalDate partitionStart = startMonth.plusMonths(i);
            LocalDate partitionEnd = partitionStart.plusMonths(1);
            String partitionName = "analytics_event_" + partitionStart.format(PARTITION_SUFFIX);
            try {
                jdbcTemplate.execute(String.format(
                        "CREATE TABLE IF NOT EXISTS %s PARTITION OF analytics_event FOR VALUES FROM ('%s') TO ('%s')",
                        partitionName, partitionStart, partitionEnd
                ));
            } catch (RuntimeException ex) {
                log.warn("event=analytics_partition_create_failed partition={}", partitionName, ex);
            }
        }
    }

    private void dropExpiredPartitions() {
        LocalDate cutoffMonth = LocalDate.now()
                .minusDays(properties.getRawRetentionDays())
                .withDayOfMonth(1);
        try {
            java.util.List<String> partitions = jdbcTemplate.queryForList(
                    """
                    SELECT c.relname FROM pg_inherits
                    JOIN pg_class c ON c.oid = pg_inherits.inhrelid
                    JOIN pg_class p ON p.oid = pg_inherits.inhparent
                    WHERE p.relname = 'analytics_event' AND c.relname <> 'analytics_event_default'
                    """,
                    String.class
            );
            for (String partition : partitions) {
                LocalDate partitionMonth = parsePartitionMonth(partition);
                if (partitionMonth != null && partitionMonth.isBefore(cutoffMonth)) {
                    jdbcTemplate.execute("DROP TABLE IF EXISTS " + partition);
                    log.info("event=analytics_partition_dropped partition={} retentionDays={}",
                            partition, properties.getRawRetentionDays());
                }
            }
        } catch (RuntimeException ex) {
            log.warn("event=analytics_partition_retention_failed", ex);
        }
    }

    private LocalDate parsePartitionMonth(String partitionName) {
        String prefix = "analytics_event_";
        if (!partitionName.startsWith(prefix)) {
            return null;
        }
        try {
            String suffix = partitionName.substring(prefix.length()) + "_01";
            return LocalDate.parse(suffix, DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
