package com.brandPitara.sfs.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.OpenStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Store opening status evaluated server-side in the business timezone. Texts are ready to render;
 * {@code nextChangeAt} lets clients refresh the badge when it flips.
 */
@Builder
public record OpeningHoursResponse(
        String timezone,
        boolean hasSchedule,
        OpenStatus status,
        String statusText,
        String todayText,
        OffsetDateTime nextChangeAt,
        List<Day> weekly
) {
    public record Day(int dayOfWeek, String label, boolean closed, List<Window> windows) {
    }

    public record Window(
            @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
            @JsonFormat(pattern = "HH:mm") LocalTime closesAt,
            boolean overnight,
            boolean fullDay
    ) {
    }
}
