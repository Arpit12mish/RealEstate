package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapeCandidateCostBreakdownDto {

    private Long id;
    private Long landCost;
    private Long constructionCost;
    private Long infrastructureCost;
    private Long otherCost;
    private Long totalCost;
    private String sourceLabel;
    private String remarks;
    private boolean verified;
}
