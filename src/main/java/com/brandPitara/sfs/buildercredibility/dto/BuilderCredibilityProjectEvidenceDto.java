package com.brandPitara.sfs.buildercredibility.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderCredibilityProjectEvidenceDto {
    private Long projectId;
    private String projectName;
    private String projectSlug;
    private String cityName;

    private Integer constructionProgressPercent;
    private String timelineStatus;
    private Integer delayDays;

    private Integer promiseFulfilmentPercent;
    private Integer complianceStrengthPercent;

    private Boolean verified;
}