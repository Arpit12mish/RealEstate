package com.brandPitara.sfs.projectcompare.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComparisonMediaItemResponse {
    private final Long id;
    private final String mediaType;
    private final String url;
    private final String thumbnailUrl;
    private final String title;
    private final String caption;
    private final Integer displayOrder;
    private final Boolean cover;
}
