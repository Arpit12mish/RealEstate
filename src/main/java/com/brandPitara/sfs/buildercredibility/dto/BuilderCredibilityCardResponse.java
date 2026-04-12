package com.brandPitara.sfs.buildercredibility.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderCredibilityCardResponse {
    private Long builderId;
    private String builderName;
    private String builderLogoUrl;
    private String cityName;

    private Integer credibilityScore;
    private String credibilityLabel;

    private String projectsTracked;
    private String onTrackRecord;
    private String promisesMet;
    private String complianceStrength;

    private String summary;
    private String confidenceLabel;
}