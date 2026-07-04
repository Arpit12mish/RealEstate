package com.brandPitara.sfs.dashboard.builderhighlight.progress.dto;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderHighlightSectionCoverageResponse {
    private BuilderHighlightType highlightType;
    private String label;
    private int buildersCovered;
    private int buildersMissing;
    private int totalItems;
    private int publishedItems;
    private int draftItems;
    private int pendingReviewItems;
}
