package com.brandPitara.sfs.dashboard.scraping.dto;

import com.brandPitara.sfs.dashboard.scraping.enums.ScrapeFieldConfidenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReraSearchSummaryDto {

    private int totalExpectedFields;
    private int foundFields;
    private int missingFields;
    private int highConfidenceFields;
    private int lowConfidenceFields;
    private int confidenceScore;
    private ScrapeFieldConfidenceStatus status;
}
