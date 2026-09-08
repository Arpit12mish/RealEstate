package com.brandPitara.sfs.analytics.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bounds for the analytics ingestion pipeline. Every limit here exists to keep
 * analytics non-blocking and non-critical per design: a full queue drops events
 * rather than back-pressuring callers, and batches are capped so one oversized
 * client payload can't dominate a drain cycle.
 *
 * Every field has a safe Java-level default, so a profile that binds none of these
 * (relying purely on defaults) still starts correctly - but production should still
 * wire the env-var placeholders explicitly (see application-prod.yml) so the contract
 * is tunable and reviewable without a code change or dependence on a gitignored file.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "sfs.analytics.ingestion")
public class AnalyticsIngestionProperties {

    /** Kill switch: when false, the ingestion endpoint accepts nothing (still 202s,
     * never errors the caller) and the nightly aggregation trigger is skipped. Manual
     * aggregation re-runs still work regardless, for incident recovery. */
    private boolean enabled = true;

    @Min(100)
    @Max(200_000)
    private int queueCapacity = 10_000;

    @Min(1)
    @Max(5_000)
    private int drainBatchSize = 300;

    @Min(100)
    @Max(30_000)
    private long drainIntervalMs = 2_000;

    @Min(64)
    @Max(8_192)
    private int maxPropertiesJsonLength = 2_000;

    @Min(1)
    @Max(3_650)
    private int rawRetentionDays = 120;

    /** How many months ahead the partition-maintenance job keeps pre-created. */
    @Min(1)
    @Max(12)
    private int partitionLookaheadMonths = 3;

    @AssertTrue(message = "drain-batch-size must not exceed queue-capacity")
    public boolean isDrainBatchSizeValid() {
        return drainBatchSize <= queueCapacity;
    }
}
