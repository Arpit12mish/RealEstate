package com.brandPitara.sfs.dashboard.builderhighlight.progress.dto;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderHighlightRecentActivityResponse {
    private Long builderId;
    private String builderName;
    private BuilderHighlightType highlightType;
    private String title;
    private BuilderHighlightStatus status;
    private OffsetDateTime updatedAt;
}
