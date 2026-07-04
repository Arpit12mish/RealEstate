package com.brandPitara.sfs.projectcompare.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComparisonProjectHeader {
    private final Long projectId;
    private final String name;
    private final String slug;
    private final String heroImageUrl;
    private final String builderName;
    private final String builderLogoUrl;
    private final String cityName;
}
