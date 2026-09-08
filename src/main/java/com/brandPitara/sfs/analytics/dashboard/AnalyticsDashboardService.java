package com.brandPitara.sfs.analytics.dashboard;

import com.brandPitara.sfs.analytics.dashboard.dto.SearchAnalyticsSummaryResponse;
import com.brandPitara.sfs.analytics.dashboard.dto.SearchQueryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Reads only from the pre-computed analytics_daily_search aggregate - never runs a
 * GROUP BY over raw analytics_event on a dashboard page load (task Step 12).
 */
@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public SearchAnalyticsSummaryResponse summary(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    coalesce(sum(search_count), 0) AS total_searches,
                    coalesce(sum(zero_result_count), 0) AS zero_results,
                    coalesce(sum(click_count), 0) AS total_clicks,
                    coalesce(avg(avg_click_position) FILTER (WHERE click_count > 0), NULL) AS avg_click_position
                FROM analytics_daily_search
                WHERE metric_date >= ? AND metric_date <= ?
                """;
        var row = jdbcTemplate.queryForMap(sql, from, to);
        long totalSearches = ((Number) row.get("total_searches")).longValue();
        long zeroResults = ((Number) row.get("zero_results")).longValue();
        long totalClicks = ((Number) row.get("total_clicks")).longValue();
        Object avgClickPosObj = row.get("avg_click_position");

        // unique_user_count is per (date, query) - not globally distinct across queries -
        // so summing it would overcount. A true cross-query unique-user count needs raw
        // events; approximated here as the max single-query unique count for now, which
        // is deliberately conservative until Phase 2 adds a proper daily-unique rollup.
        String uniqueUsersSql = """
                SELECT coalesce(max(unique_user_count), 0) FROM analytics_daily_search
                WHERE metric_date >= ? AND metric_date <= ?
                """;
        Long uniqueUsers = jdbcTemplate.queryForObject(uniqueUsersSql, Long.class, from, to);

        double zeroResultRate = totalSearches == 0 ? 0.0 : (double) zeroResults / totalSearches;
        double ctr = totalSearches == 0 ? 0.0 : (double) totalClicks / totalSearches;

        return SearchAnalyticsSummaryResponse.builder()
                .totalSearches(totalSearches)
                .uniqueSearchUsers(uniqueUsers == null ? 0 : uniqueUsers)
                .zeroResultRate(zeroResultRate)
                .searchCtr(ctr)
                .avgClickedPosition(avgClickPosObj == null ? null : ((Number) avgClickPosObj).doubleValue())
                .build();
    }

    public List<SearchQueryRow> topSearches(LocalDate from, LocalDate to, int limit) {
        return queryRows(from, to, limit, "searches DESC");
    }

    public List<SearchQueryRow> zeroResultSearches(LocalDate from, LocalDate to, int limit) {
        String sql = baseRowSql() + " HAVING sum(zero_result_count) > 0 ORDER BY zero_results DESC LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRow, from, to, limit);
    }

    public List<SearchQueryRow> lowCtrSearches(LocalDate from, LocalDate to, int minSearches, int limit) {
        String sql = baseRowSql()
                + " HAVING sum(search_count) >= ? ORDER BY (sum(click_count)::numeric / NULLIF(sum(search_count), 0)) ASC LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRow, from, to, minSearches, limit);
    }

    private List<SearchQueryRow> queryRows(LocalDate from, LocalDate to, int limit, String orderBy) {
        String sql = baseRowSql() + " ORDER BY " + orderBy + " LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRow, from, to, limit);
    }

    private String baseRowSql() {
        return """
                SELECT
                    normalized_query,
                    sum(search_count) AS searches,
                    max(unique_user_count) AS unique_users,
                    avg(avg_result_count) AS avg_result_count,
                    sum(click_count) AS clicks,
                    sum(zero_result_count) AS zero_results,
                    avg(avg_click_position) FILTER (WHERE click_count > 0) AS avg_click_position
                FROM analytics_daily_search
                WHERE metric_date >= ? AND metric_date <= ?
                GROUP BY normalized_query
                """;
    }

    private SearchQueryRow mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        long searches = rs.getLong("searches");
        long clicks = rs.getLong("clicks");
        Double avgResultCount = (Double) rs.getObject("avg_result_count");
        Double avgClickPosition = (Double) rs.getObject("avg_click_position");
        return SearchQueryRow.builder()
                .query(rs.getString("normalized_query"))
                .searches(searches)
                .uniqueUsers(rs.getLong("unique_users"))
                .avgResultCount(avgResultCount)
                .clicks(clicks)
                .ctr(searches == 0 ? 0.0 : (double) clicks / searches)
                .avgClickPosition(avgClickPosition)
                .build();
    }
}
