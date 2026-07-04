package com.brandPitara.sfs.dashboard.builderhighlight.progress.dto;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.enums.BuilderHighlightSectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderHighlightSectionProgressResponse {
    private BuilderHighlightType highlightType;
    private String label;
    private boolean required;
    private boolean present;
    private int totalItemCount;
    private int draftCount;
    private int pendingReviewCount;
    private int publishedCount;
    private int publishedPublicItemCount;
    private OffsetDateTime latestUpdatedAt;
    private BuilderHighlightSectionStatus status;
}
