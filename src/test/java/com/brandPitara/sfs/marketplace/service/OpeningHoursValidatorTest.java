package com.brandPitara.sfs.marketplace.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpeningHoursValidatorTest {

    private static OpeningHoursCalculator.Interval i(int day, String open, String close) {
        return new OpeningHoursCalculator.Interval(day, LocalTime.parse(open), LocalTime.parse(close));
    }

    @Test
    void splitShiftsOnTheSameDayAreAllowed() {
        assertThatCode(() -> OpeningHoursValidator.validate(List.of(i(1, "09:00", "13:00"), i(1, "16:00", "21:00"))))
                .doesNotThrowAnyException();
    }

    @Test
    void touchingIntervalsDoNotOverlap() {
        assertThatCode(() -> OpeningHoursValidator.validate(List.of(i(3, "18:00", "00:00"), i(4, "00:00", "03:00"))))
                .doesNotThrowAnyException();
    }

    @Test
    void overlappingIntervalsAreRejected() {
        assertThatThrownBy(() -> OpeningHoursValidator.validate(List.of(i(1, "09:00", "13:00"), i(1, "12:00", "18:00"))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void overnightIntervalOverlappingNextMorningIsRejected() {
        assertThatThrownBy(() -> OpeningHoursValidator.validate(List.of(i(2, "20:00", "04:00"), i(3, "03:00", "10:00"))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void sundayOvernightWrapsIntoMonday() {
        assertThatThrownBy(() -> OpeningHoursValidator.validate(List.of(i(7, "22:00", "06:00"), i(1, "05:00", "12:00"))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void fullDayConflictsWithAnyOtherIntervalThatDay() {
        assertThatThrownBy(() -> OpeningHoursValidator.validate(List.of(i(5, "00:00", "00:00"), i(5, "10:00", "11:00"))))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void invalidDayAndTooManyIntervalsAreRejected() {
        assertThatThrownBy(() -> OpeningHoursValidator.validate(List.of(i(8, "09:00", "10:00"))))
                .hasMessageContaining("dayOfWeek");

        List<OpeningHoursCalculator.Interval> many = new ArrayList<>();
        for (int n = 0; n < 22; n++) many.add(i(1 + (n % 7), "0" + (n / 7) + ":00", "0" + (n / 7) + ":30"));
        assertThatThrownBy(() -> OpeningHoursValidator.validate(many)).hasMessageContaining("At most");
    }
}
