package com.brandPitara.sfs.analytics.dashboard;

import com.brandPitara.sfs.analytics.aggregation.AnalyticsAggregationScheduler;
import com.brandPitara.sfs.analytics.dashboard.dto.SearchAnalyticsSummaryResponse;
import com.brandPitara.sfs.analytics.dashboard.dto.SearchQueryRow;
import com.brandPitara.sfs.analytics.security.AnalyticsAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Phase 1 dashboard read surface: Search Analytics only (task Step 23/16). Gated on
 * DashboardPermission.ANALYTICS_VIEW via AnalyticsAccessPolicy, the same
 * has(authentication, permission) shape CmsContentAccessPolicy already established -
 * this is a coarse aggregate-only view, not the individual session drill-down (that's
 * ANALYTICS_VIEW_USER_LEVEL, Phase 3).
 */
@RestController
@RequestMapping("/api/dashboard/analytics/search")
@RequiredArgsConstructor
public class AnalyticsDashboardController {

    /**
     * Backend-optimization pass: aggregate rows are retained indefinitely (only raw
     * events are pruned by retention), so nothing previously stopped a dashboard user
     * from requesting an unbounded date range (e.g. decades). The aggregate table is
     * small enough that this was never a correctness risk, but it's an easy, free
     * defensive cap - matches the existing boundedLimit() clamp-not-reject convention.
     */
    private static final long MAX_RANGE_DAYS = 366;

    private final AnalyticsDashboardService dashboardService;
    private final AnalyticsAccessPolicy accessPolicy;
    private final AnalyticsAggregationScheduler aggregationScheduler;

    @GetMapping("/summary")
    public SearchAnalyticsSummaryResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        accessPolicy.assertCanView(authentication);
        return dashboardService.summary(boundedFrom(from, to), to);
    }

    @GetMapping("/top")
    public List<SearchQueryRow> top(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        accessPolicy.assertCanView(authentication);
        return dashboardService.topSearches(boundedFrom(from, to), to, boundedLimit(limit));
    }

    @GetMapping("/zero-results")
    public List<SearchQueryRow> zeroResults(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        accessPolicy.assertCanView(authentication);
        return dashboardService.zeroResultSearches(boundedFrom(from, to), to, boundedLimit(limit));
    }

    @GetMapping("/low-ctr")
    public List<SearchQueryRow> lowCtr(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") int minSearches,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        accessPolicy.assertCanView(authentication);
        return dashboardService.lowCtrSearches(boundedFrom(from, to), to, Math.max(1, minSearches), boundedLimit(limit));
    }

    /**
     * Manual backfill/retry trigger - the nightly job only ever covers "yesterday"
     * (see AnalyticsAggregationScheduler), so an ops-driven re-run for an arbitrary date
     * needs its own path. Mutating, so gated on ADMIN directly rather than the
     * view-only ANALYTICS_VIEW permission, matching this codebase's existing convention
     * for admin-only write endpoints (e.g. AdminProjectMeterSnapshotController).
     */
    @PostMapping("/aggregation/run")
    @PreAuthorize("hasRole('ADMIN')")
    public void runAggregation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        aggregationScheduler.runFor(date);
    }

    private int boundedLimit(int limit) {
        return Math.min(Math.max(limit, 1), 100);
    }

    /** Clamps an over-wide range from the near end - "to" stays authoritative (what the
     * caller actually wants to see up to), "from" is pulled forward if the span is too large. */
    private LocalDate boundedFrom(LocalDate from, LocalDate to) {
        LocalDate earliestAllowed = to.minusDays(MAX_RANGE_DAYS);
        return from.isBefore(earliestAllowed) ? earliestAllowed : from;
    }
}
