package com.brandPitara.sfs.analytics.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchAnalyticsSummaryResponse {
    private long totalSearches;
    private long uniqueSearchUsers;
    private double zeroResultRate;
    private double searchCtr;
    private Double avgClickedPosition;
}
