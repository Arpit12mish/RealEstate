package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/**
 * Full weekly schedule. Omit a day to mark it closed. closesAt before opensAt = overnight;
 * closesAt equal to opensAt = open 24 hours from opensAt.
 */
public record OpeningHoursReplaceRequest(
        @NotNull @Size(max = 21) List<@Valid @NotNull Interval> intervals
) {
    public record Interval(
            @NotNull @Min(1) @Max(7) Integer dayOfWeek,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime closesAt
    ) {
    }
}
