package com.brandPitara.sfs.marketplace.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.List;

/**
 * Rejects schedules that cannot be evaluated unambiguously: bad day numbers, too many intervals,
 * or intervals that overlap anywhere in the week (including an overnight Sunday interval running
 * into Monday).
 */
public final class OpeningHoursValidator {

    public static final int MAX_INTERVALS = 21;

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY;

    private OpeningHoursValidator() {
    }

    public static void validate(List<OpeningHoursCalculator.Interval> intervals) {
        if (intervals.size() > MAX_INTERVALS) {
            throw badRequest("At most " + MAX_INTERVALS + " opening intervals are allowed");
        }
        int[][] ranges = new int[intervals.size()][];
        for (int i = 0; i < intervals.size(); i++) {
            OpeningHoursCalculator.Interval in = intervals.get(i);
            if (in.dayOfWeek() < 1 || in.dayOfWeek() > 7) {
                throw badRequest("dayOfWeek must be between 1 (Monday) and 7 (Sunday)");
            }
            if (in.opensAt() == null || in.closesAt() == null) {
                throw badRequest("opensAt and closesAt are required");
            }
            int start = (in.dayOfWeek() - 1) * MINUTES_PER_DAY + in.opensAt().toSecondOfDay() / 60;
            ranges[i] = new int[]{start, start + durationMinutes(in)};
        }

        for (int a = 0; a < ranges.length; a++) {
            for (int b = a + 1; b < ranges.length; b++) {
                if (overlaps(ranges[a], ranges[b])) {
                    throw badRequest("Opening intervals overlap: " + describe(intervals.get(a)) + " and "
                            + describe(intervals.get(b)));
                }
            }
        }
    }

    static int durationMinutes(OpeningHoursCalculator.Interval in) {
        if (in.fullDay()) return MINUTES_PER_DAY;
        long minutes = Duration.between(in.opensAt(), in.closesAt()).toMinutes();
        return (int) (in.overnight() ? minutes + MINUTES_PER_DAY : minutes);
    }

    /** Overlap on a circular week: compare against the other range shifted by one week each way. */
    private static boolean overlaps(int[] x, int[] y) {
        for (int shift = -MINUTES_PER_WEEK; shift <= MINUTES_PER_WEEK; shift += MINUTES_PER_WEEK) {
            int ys = y[0] + shift;
            int ye = y[1] + shift;
            if (x[0] < ye && ys < x[1]) return true;
        }
        return false;
    }

    private static String describe(OpeningHoursCalculator.Interval in) {
        return DayOfWeek.of(in.dayOfWeek()) + " " + in.opensAt() + "-" + in.closesAt();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
