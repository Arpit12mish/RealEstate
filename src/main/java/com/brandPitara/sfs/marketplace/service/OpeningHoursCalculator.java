package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.marketplace.dto.OpeningHoursResponse;
import com.brandPitara.sfs.marketplace.enums.OpenStatus;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Evaluates a weekly opening schedule in the business's own timezone.
 *
 * <p>Interval semantics (same as {@code business_opening_hours}): ISO day of week (1 = Monday);
 * {@code closesAt} before {@code opensAt} closes on the following day (overnight);
 * {@code closesAt} equal to {@code opensAt} is open for a full 24 hours; a day without intervals
 * is closed. Adjacent or overlapping intervals (e.g. Mon 18:00-02:00 and Tue 00:00-09:00) are
 * merged before evaluation, so "closes at" is the real end of continuous opening.
 */
public final class OpeningHoursCalculator {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter TILE_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter STATUS_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private OpeningHoursCalculator() {
    }

    public record Interval(int dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        public boolean overnight() {
            return closesAt.isBefore(opensAt);
        }

        public boolean fullDay() {
            return closesAt.equals(opensAt);
        }
    }

    private record Span(ZonedDateTime start, ZonedDateTime end) {
    }

    public static ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) return DEFAULT_ZONE;
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException ex) {
            return DEFAULT_ZONE;
        }
    }

    /**
     * Legacy rows only have a daily open/close time; treat them as the same interval every day.
     */
    public static List<Interval> legacyDaily(LocalTime openTime, LocalTime closeTime) {
        if (openTime == null || closeTime == null) return List.of();
        List<Interval> out = new ArrayList<>(7);
        for (int d = 1; d <= 7; d++) out.add(new Interval(d, openTime, closeTime));
        return out;
    }

    public static OpeningHoursResponse evaluate(List<Interval> intervals, ZoneId zone, Instant now) {
        List<Interval> sorted = intervals == null ? List.of() : intervals.stream()
                .sorted(Comparator.comparingInt(Interval::dayOfWeek).thenComparing(Interval::opensAt))
                .toList();

        List<OpeningHoursResponse.Day> weekly = weekly(sorted);
        ZonedDateTime localNow = now.atZone(zone);

        if (sorted.isEmpty()) {
            return OpeningHoursResponse.builder()
                    .timezone(zone.getId())
                    .hasSchedule(false)
                    .status(OpenStatus.UNKNOWN)
                    .statusText(null)
                    .todayText(null)
                    .nextChangeAt(null)
                    .weekly(weekly)
                    .build();
        }

        List<Span> spans = merge(expand(sorted, zone, localNow.toLocalDate()));
        Span current = spans.stream()
                .filter(s -> !s.start().isAfter(localNow) && s.end().isAfter(localNow))
                .findFirst()
                .orElse(null);

        OpenStatus status;
        String statusText;
        ZonedDateTime nextChange;

        if (current != null) {
            status = OpenStatus.OPEN;
            if (Duration.between(localNow, current.end()).toDays() >= 7) {
                // Continuous coverage across the whole expansion window: open around the clock.
                statusText = "Open 24 hours";
                nextChange = null;
            } else {
                nextChange = current.end();
                statusText = "Closes " + relativeDay(localNow, nextChange) + STATUS_TIME.format(nextChange);
            }
        } else {
            status = OpenStatus.CLOSED;
            nextChange = spans.stream()
                    .map(Span::start)
                    .filter(start -> start.isAfter(localNow))
                    .findFirst()
                    .orElse(null);
            statusText = nextChange == null
                    ? "Closed"
                    : "Opens " + relativeDay(localNow, nextChange) + STATUS_TIME.format(nextChange);
        }

        return OpeningHoursResponse.builder()
                .timezone(zone.getId())
                .hasSchedule(true)
                .status(status)
                .statusText(statusText)
                .todayText(todayText(sorted, localNow.getDayOfWeek()))
                .nextChangeAt(nextChange == null ? null : nextChange.toOffsetDateTime())
                .weekly(weekly)
                .build();
    }

    /** "9:00 am to 10:00 pm" for the current local day, as shown on the Timing tile. */
    static String todayText(List<Interval> sorted, DayOfWeek today) {
        List<Interval> todays = sorted.stream()
                .filter(i -> i.dayOfWeek() == today.getValue())
                .toList();
        if (todays.isEmpty()) return "Closed today";
        if (todays.stream().anyMatch(Interval::fullDay)) return "Open 24 hours";
        return String.join(", ", todays.stream()
                .map(i -> tileTime(i.opensAt()) + " to " + tileTime(i.closesAt()))
                .toList());
    }

    private static String tileTime(LocalTime t) {
        return TILE_TIME.format(t).toLowerCase(Locale.ENGLISH);
    }

    /** "" for today, "tomorrow at " / "Mon at " otherwise, so texts read "Opens tomorrow at 9:00 AM". */
    private static String relativeDay(ZonedDateTime now, ZonedDateTime target) {
        LocalDate today = now.toLocalDate();
        LocalDate day = target.toLocalDate();
        if (day.equals(today)) return "at ";
        if (day.equals(today.plusDays(1))) return "tomorrow at ";
        return day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " at ";
    }

    /** Concrete spans from yesterday through a week ahead, so overnight carry-over is covered. */
    private static List<Span> expand(List<Interval> sorted, ZoneId zone, LocalDate today) {
        List<Span> spans = new ArrayList<>();
        for (int offset = -1; offset <= 7; offset++) {
            LocalDate date = today.plusDays(offset);
            int iso = date.getDayOfWeek().getValue();
            for (Interval i : sorted) {
                if (i.dayOfWeek() != iso) continue;
                // ZonedDateTime.of shifts times that fall in a DST gap forward, which is the
                // correct reading of "opens at 02:30" on a spring-forward day.
                ZonedDateTime start = ZonedDateTime.of(date, i.opensAt(), zone);
                ZonedDateTime end;
                if (i.fullDay()) {
                    end = start.plusDays(1);
                } else if (i.overnight()) {
                    end = ZonedDateTime.of(date.plusDays(1), i.closesAt(), zone);
                } else {
                    end = ZonedDateTime.of(date, i.closesAt(), zone);
                }
                spans.add(new Span(start, end));
            }
        }
        return spans;
    }

    private static List<Span> merge(List<Span> spans) {
        List<Span> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator.comparing(s -> s.start().toInstant()));
        List<Span> merged = new ArrayList<>();
        for (Span s : sorted) {
            if (!merged.isEmpty()) {
                Span last = merged.get(merged.size() - 1);
                if (!s.start().isAfter(last.end())) {
                    if (s.end().isAfter(last.end())) {
                        merged.set(merged.size() - 1, new Span(last.start(), s.end()));
                    }
                    continue;
                }
            }
            merged.add(s);
        }
        return merged;
    }

    private static List<OpeningHoursResponse.Day> weekly(List<Interval> sorted) {
        List<OpeningHoursResponse.Day> days = new ArrayList<>(7);
        for (int d = 1; d <= 7; d++) {
            final int day = d;
            List<OpeningHoursResponse.Window> windows = sorted.stream()
                    .filter(i -> i.dayOfWeek() == day)
                    .map(i -> new OpeningHoursResponse.Window(i.opensAt(), i.closesAt(), i.overnight(), i.fullDay()))
                    .toList();
            days.add(new OpeningHoursResponse.Day(
                    day,
                    DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    windows.isEmpty(),
                    windows
            ));
        }
        return days;
    }

    /** Whole years since establishment in the business's zone; null if unknown or in the future. */
    public static Integer yearsInBusiness(Integer establishedYear, ZoneId zone, Instant now) {
        if (establishedYear == null) return null;
        int currentYear = now.atZone(zone).getYear();
        if (establishedYear > currentYear) return null;
        return currentYear - establishedYear;
    }
}
