package com.brandPitara.sfs.dashboard.builderhighlight.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderHighlightOverviewResponse {
    private long totalBuilders;
    private long buildersWithAnyHighlights;
    private long buildersWithAllRequiredSections;
    private long buildersMissingHighlights;
    private long totalHighlightItems;
    private long publishedItems;
    private long draftItems;
    private long pendingReviewItems;
    private long archivedItems;
    private List<BuilderHighlightSectionCoverageResponse> sectionCoverage;
    private List<BuilderHighlightRecentActivityResponse> recentActivity;
}
