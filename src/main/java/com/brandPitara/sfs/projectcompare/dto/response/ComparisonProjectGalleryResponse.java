package com.brandPitara.sfs.projectcompare.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ComparisonProjectGalleryResponse {
    private final Long projectId;
    private final String projectName;
    private final String coverImageUrl;
    private final Integer totalCount;

    @Builder.Default
    private final List<ComparisonMediaItemResponse> items = List.of();
}
