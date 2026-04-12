package com.brandPitara.sfs.buildercredibility.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderCredibilitySummaryResponse {
    private Long builderId;
    private String builderName;
    private String builderLogoUrl;
    private String cityName;

    private Integer credibilityScore;
    private String credibilityLabel;

    private Integer projectsTrackedCount;
    private Double onTrackRecordPercent;
    private Double promisesMetPercent;
    private Double complianceStrengthPercent;

    private String summary;
    private String confidenceLabel;
}