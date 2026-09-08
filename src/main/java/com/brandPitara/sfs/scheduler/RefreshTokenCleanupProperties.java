package com.brandPitara.sfs.scheduler;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "sfs.refresh-token.cleanup")
public class RefreshTokenCleanupProperties {

    private boolean enabled = true;

    @NotBlank
    private String schedule = "0 0 2 * * *";

    @Min(1)
    @Max(10_000)
    private int batchSize = 1_000;

    @Min(1)
    @Max(10_000)
    private int maximumBatchesPerRun = 20;

    @NotNull
    private Duration maximumExecutionDuration = Duration.ofSeconds(30);

    @AssertTrue(message = "schedule must be a valid Spring cron expression")
    public boolean isScheduleValid() {
        return schedule != null && CronExpression.isValidExpression(schedule);
    }

    @AssertTrue(message = "maximumExecutionDuration must be between 1 millisecond and 24 hours")
    public boolean isMaximumExecutionDurationValid() {
        return maximumExecutionDuration != null
                && maximumExecutionDuration.compareTo(Duration.ofMillis(1)) >= 0
                && maximumExecutionDuration.compareTo(Duration.ofHours(24)) <= 0;
    }
}
