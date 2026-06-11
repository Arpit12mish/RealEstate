package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementIssueMomentumResponse {

    /**
     * Main card.
     */
    private String title;
    private String subtitle;
    private String infoText;

    /**
     * Detail page header.
     */
    private String detailTitle;
    private String detailSubtitle;

    /**
     * Main card + detail top cards.
     */
    private String periodLabel;
    private String totalCardLabel;
    private Long problemsBefore;

    private String averageTimeLabel;
    private Integer averageResolutionDays;
    private String averageResolutionDaysLabel;

    private Long problemsLeft;
    private Long issuesResolved;
    private Integer resolutionPercent;
    private String resolutionTrendLabel;
    private String calculationText;

    /**
     * Detail page graph.
     */
    private List<BuilderImprovementIssueTrendPointResponse> trend;

    /**
     * Detail page tabs.
     */
    private String resolvedTabLabel;
    private String inProgressTabLabel;

    private List<BuilderImprovementIssueResponse> resolvedIssues;
    private List<BuilderImprovementIssueResponse> inProgressIssues;

    /**
     * Total (6MO) bottom sheet.
     */
    private String issueHistorySheetTitle;
    private List<BuilderImprovementIssueResponse> issueHistory;

    /**
     * Footer.
     */
    private String footerText;
    private OffsetDateTime lastUpdatedAt;
}