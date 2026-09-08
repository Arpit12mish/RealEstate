package com.brandPitara.sfs.analytics.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

/** One row of the Top Searches / Zero-Result Searches / Low-CTR Searches tables. */
@Getter
@Builder
public class SearchQueryRow {
    private String query;
    private long searches;
    private long uniqueUsers;
    private Double avgResultCount;
    private long clicks;
    private Double ctr;
    private Double avgClickPosition;
}
