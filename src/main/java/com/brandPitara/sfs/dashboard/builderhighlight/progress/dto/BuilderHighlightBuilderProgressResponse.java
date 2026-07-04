package com.brandPitara.sfs.dashboard.builderhighlight.progress.dto;

import com.brandPitara.sfs.dashboard.builderhighlight.progress.enums.BuilderHighlightOverallProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderHighlightBuilderProgressResponse {
    private Long builderId;
    private String builderName;
    private boolean hasAnyHighlights;
    private boolean hasPublishedPublicHighlights;
    private boolean highlightsAvailableForMobile;
    private int requiredSectionCount;
    private int completedSectionCount;
    private int missingSectionCount;
    private int completionPercent;
    private BuilderHighlightOverallProgressStatus overallStatus;
    private List<BuilderHighlightSectionProgressResponse> sections;
}
