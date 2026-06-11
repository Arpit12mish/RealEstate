package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementResponse {

    private Long profileId;

    private Long builderId;
    private String builderName;
    private String builderLogoUrl;

    private Long projectId;
    private String projectName;
    private String projectSlug;

    private String badgeText;
    private String heroTitle;
    private String heroSubtitle;
    private String heroImageUrl;
    private String verifiedByText;
    private String summary;

    private String overallStatus;
    private String evidenceStatus;
    private OffsetDateTime lastReviewedAt;

    private BuilderImprovementBuilderResponseDto builderResponse;

    private List<BuilderImprovementActionResponse> actions;
    private BuilderImprovementIssueMomentumResponse issueClosureMomentum;
    private List<BuilderAfterSalesUpgradeResponse> afterSalesUpgrades;
    private List<BuilderImprovementTimelineResponse> timeline;
}