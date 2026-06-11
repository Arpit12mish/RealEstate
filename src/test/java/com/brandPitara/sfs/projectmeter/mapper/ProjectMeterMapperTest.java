package com.brandPitara.sfs.projectmeter.mapper;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMeterMapperTest {

    @Test
    void completedWithinLatestReraTimelineShowsExtensionLabel() {
        var result = ProjectMeterMapper.computeTimeline(
            LocalDate.of(2023, 6, 30),
            LocalDate.of(2024, 6, 30),
            LocalDate.of(2024, 3, 31),
            2
        );

        assertThat(result.status()).isEqualTo("COMPLETED_WITHIN_REVISED_RERA");
        assertThat(result.label()).isEqualTo("Completed after 2 RERA extensions");
        assertThat(result.delayVsOriginalDays()).isPositive();
        assertThat(result.delayVsLatestReraDays()).isZero();
    }

    @Test
    void activeProjectWithFutureLatestReraDateShowsExtendedOnTrack() {
        var result = ProjectMeterMapper.computeTimeline(
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(90),
            null,
            2
        );

        assertThat(result.status()).isEqualTo("EXTENDED_ON_TRACK");
        assertThat(result.label()).isEqualTo("RERA timeline extended 2 times");
    }

    @Test
    void activeProjectPastLatestReraDateShowsDelayed() {
        var result = ProjectMeterMapper.computeTimeline(
            LocalDate.now().minusDays(120),
            LocalDate.now().minusDays(10),
            null,
            1
        );

        assertThat(result.status()).isEqualTo("DELAYED");
        assertThat(result.label()).isEqualTo("10d Delayed");
    }
}
