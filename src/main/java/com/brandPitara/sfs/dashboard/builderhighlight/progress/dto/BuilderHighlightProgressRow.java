package com.brandPitara.sfs.dashboard.builderhighlight.progress.dto;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;

import java.time.OffsetDateTime;

/**
 * Lightweight, non-entity projection used to aggregate Builder Highlight progress
 * with a single bulk query instead of loading full item graphs per builder.
 */
public record BuilderHighlightProgressRow(
    Long builderId,
    String builderName,
    BuilderHighlightType highlightType,
    BuilderHighlightStatus status,
    Boolean publicVisible,
    Boolean active,
    OffsetDateTime updatedAt,
    String title
) {
    public boolean isPubliclyVisible() {
        return status == BuilderHighlightStatus.PUBLISHED
            && Boolean.TRUE.equals(publicVisible)
            && Boolean.TRUE.equals(active);
    }
}
