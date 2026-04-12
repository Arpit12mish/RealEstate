package com.brandPitara.sfs.buildercredibility.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderCredibilityResponse {
    private Long builderId;
    private String builderName;
    private String builderLogoUrl;
    private String cityName;

    private Integer credibilityScore;
    private String credibilityLabel;
    private String summary;
    private String confidenceLabel;

    private Integer trackedProjectsCount;

    private List<BuilderCredibilityMetricDto> metrics;
    private List<BuilderCredibilityScoreBreakdownDto> scoreBreakdown;
    private List<BuilderCredibilityProjectEvidenceDto> recentProjectEvidence;
    private List<BuilderCredibilityIndicatorDto> positiveIndicators;
    private List<BuilderCredibilityRiskDto> observedRisks;
}