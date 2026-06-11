package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementSummaryResponse {

    private Long builderId;
    private String builderName;
    private String builderLogoUrl;

    private Boolean enabled;

    private Long profileId;
    private String badgeText;
    private String ctaText;
    private String headline;
    private String overallStatus;
    private String evidenceStatus;
    private OffsetDateTime lastReviewedAt;
}