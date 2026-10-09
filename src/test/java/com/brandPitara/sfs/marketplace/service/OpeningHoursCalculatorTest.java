package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.marketplace.dto.OpeningHoursResponse;
import com.brandPitara.sfs.marketplace.enums.OpenStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpeningHoursCalculatorTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static Instant ist(int y, int m, int d, int h, int min) {
        return ZonedDateTime.of(y, m, d, h, min, 0, 0, IST).toInstant();
    }

    private static List<OpeningHoursCalculator.Interval> everyDay(String open, String close) {
        List<OpeningHoursCalculator.Interval> out = new ArrayList<>();
        for (int d = 1; d <= 7; d++) out.add(new OpeningHoursCalculator.Interval(d, LocalTime.parse(open), LocalTime.parse(close)));
        return out;
    }

    // 2026-10-07 is a Wednesday (ISO 3).

    @Test
    void openDuringIntervalReportsClosingTimeInBusinessZone() {
        OpeningHoursResponse r = OpeningHoursCalculator.evaluate(everyDay("09:00", "22:00"), IST, ist(2026, 10, 7, 12, 0));

        assertThat(r.status()).isEqualTo(OpenStatus.OPEN);
        assertThat(r.statusText()).isEqualTo("Closes at 10:00 PM");
        assertThat(r.todayText()).isEqualTo("9:00 am to 10:00 pm");
        assertThat(r.nextChangeAt().toInstant()).isEqualTo(ist(2026, 10, 7, 22, 0));
        assertThat(r.hasSchedule()).isTrue();
    }

    @Test
    void evaluatesInBusinessZoneNotUtc() {
        // 03:00 UTC is 08:30 IST: closed in India even though it is "after 3am" in UTC terms.
        Instant now = Instant.parse("2026-10-07T03:00:00Z");
        OpeningHoursResponse r = OpeningHoursCalculator.evaluate(everyDay("09:00", "22:00"), IST, now);

        assertThat(r.status()).isEqualTo(OpenStatus.CLOSED);
        assertThat(r.statusText()).isEqualTo("Opens at 9:00 AM");
    }

    @Test
    void afterClosingOpensTomorrow() {
        OpeningHoursResponse r = OpeningHoursCalculator.evaluate(everyDay("09:00", "22:00"), IST, ist(2026, 10, 7, 23, 0));

        assertThat(r.status()).isEqualTo(OpenStatus.CLOSED);
        assertThat(r.statusText()).isEqualTo("Opens tomorrow at 9:00 AM");
    }

    @Test
    void overnightIntervalStaysOpenPastMidnightAndClosesNextDay() {
        // Tuesday 18:00 -> Wednesday 02:00. At Wednesday 01:00 the Tuesday interval is still open.
        List<OpeningHoursCalculator.Interval> hours = List.of(
                new OpeningHoursCalculator.Interval(2, LocalTime.of(18, 0), LocalTime.of(2, 0)));

        OpeningHoursResponse r = OpeningHoursCalculator.evaluate(hours, IST, ist(2026, 10, 7, 1, 0));

        assertThat(r.status()).isEqualTo(OpenStatus.OPEN);
        assertThat(r.statusText()).isEqualTo("Closes at 2:00 AM");
        assertThat(r.weekly().get(1).windows().get(0).overnight()).isTrue();
        // Wednesday itself has no interval of its own.
        assertThat(r.todayText()).isEqualTo("Closed today");
    }

    @Test
    void closedDayIsSkippedWhenFindingNextOpening() {
        // Open Mon-Sat, closed Sunday. Saturday 2026-10-10 at 23:00 -> next opening Monday.
        List<OpeningHoursCalculator.Interval> hours = new ArrayList<>();
        for (int d = 1; d <= 6; d++) hours.add(new OpeningHoursCalculator.Interval(d, LocalTime.of(9, 0), LocalTime.of(21, 0)));

        OpeningHoursResponse r = OpeningHoursCalculator.evaluate(hours, IST, ist(2026, 10, 10, 23, 0));

        assertThat(r.status()).isEqualTo(OpenStatus.CLOSED);
        assertThat(r.statusText()).isEqualTo("Opens Mon at 9:00 AM");
        assertThat(r.weekly().get(6).closed()).isTrue();
        assertThat(r.weekly().get(6).label()).isEqualTo("Sun");
    }

    @Test
    void adjacentOvernightIntervalsMergeIntoOneClosingTime() {
        // Wed 18:00-00:00 (overnight to Thu 00:00) and Thu 00:00-03:00: continuous until Thu 03:00.
        List<OpeningHoursCalculator.Interval> hours = List.of(
                new OpeningHoursCalculator.Interval(3, LocalTime.of(18, 0), LocalTime.MIDNIGHT),
                new OpeningHoursCalculator.Interval(4, LocalTime.MIDNIGHT, LocalTime.of(3, 0)));

        OpeningHoursResponse r = OpeningHoursCalculator.evaluate(hours, IST, ist(2026, 10, 7, 23, 0));

        assertThat(r.status()).isEqualTo(OpenStatus.OPEN);
        assertThat(r.statusText()).isEqualTo("Closes tomorrow at 3:00 AM");
    }

    @Test
    void equalOpenAndCloseEveryDayIsOpenAroundTheClock() {
        OpeningHoursResponse r = OpeningHoursCalculator.evaluate(everyDay("00:00", "00:00"), IST, ist(2026, 10, 7, 4, 0));

        assertThat(r.status()).isEqualTo(OpenStatus.OPEN);
        assertThat(r.statusText()).isEqualTo("Open 24 hours");
        assertThat(r.nextChangeAt()).isNull();
        assertThat(r.todayText()).isEqualTo("Open 24 hours");
    }

    @Test
    void noScheduleIsUnknownRatherThanClosed() {
        OpeningHoursResponse r = OpeningHoursCalculator.evaluate(List.of(), IST, ist(2026, 10, 7, 12, 0));

        assertThat(r.status()).isEqualTo(OpenStatus.UNKNOWN);
        assertThat(r.hasSchedule()).isFalse();
        assertThat(r.statusText()).isNull();
        assertThat(r.weekly()).hasSize(7).allMatch(OpeningHoursResponse.Day::closed);
    }

    @Test
    void legacyDailyTimesApplyToEveryDay() {
        List<OpeningHoursCalculator.Interval> legacy = OpeningHoursCalculator.legacyDaily(LocalTime.of(10, 0), LocalTime.of(20, 0));

        assertThat(legacy).hasSize(7);
        assertThat(OpeningHoursCalculator.legacyDaily(null, LocalTime.NOON)).isEmpty();
    }

    @Test
    void springForwardGapShiftsOpeningForward() {
        ZoneId newYork = ZoneId.of("America/New_York");
        // 2026-03-08 02:30 does not exist in New York (clocks jump 02:00 -> 03:00). Sunday = 7.
        List<OpeningHoursCalculator.Interval> hours = List.of(
                new OpeningHoursCalculator.Interval(7, LocalTime.of(2, 30), LocalTime.of(10, 0)));
        // The nonexistent 02:30 opening is read as 03:30 local time.
        OpeningHoursResponse before = OpeningHoursCalculator.evaluate(hours, newYork,
                ZonedDateTime.of(2026, 3, 8, 3, 15, 0, 0, newYork).toInstant());
        OpeningHoursResponse after = OpeningHoursCalculator.evaluate(hours, newYork,
                ZonedDateTime.of(2026, 3, 8, 3, 45, 0, 0, newYork).toInstant());

        assertThat(before.status()).isEqualTo(OpenStatus.CLOSED);
        assertThat(before.statusText()).isEqualTo("Opens at 3:30 AM");
        assertThat(after.status()).isEqualTo(OpenStatus.OPEN);
        assertThat(after.timezone()).isEqualTo("America/New_York");
    }

    @Test
    void invalidZoneFallsBackToIndia() {
        assertThat(OpeningHoursCalculator.resolveZone("Mars/Base")).isEqualTo(IST);
        assertThat(OpeningHoursCalculator.resolveZone(null)).isEqualTo(IST);
    }

    @Test
    void yearsInBusinessUsesBusinessZoneAndRejectsFutureYears() {
        // 2026-12-31 20:00 UTC is already 2027 in India.
        Instant newYearInIndia = Instant.parse("2026-12-31T20:00:00Z");

        assertThat(OpeningHoursCalculator.yearsInBusiness(2008, IST, newYearInIndia)).isEqualTo(19);
        assertThat(OpeningHoursCalculator.yearsInBusiness(2030, IST, newYearInIndia)).isNull();
        assertThat(OpeningHoursCalculator.yearsInBusiness(null, IST, newYearInIndia)).isNull();
    }
}
