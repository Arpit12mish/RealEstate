package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderAfterSalesUpgradeResponse {

    private Long id;

    /**
     * Card title.
     * Example: Slow defect resolution
     */
    private String cardProblem;

    /**
     * Card subtitle/action.
     * Example: Dedicated team added for 48h turnaround
     */
    private String cardAction;

    private String iconKey;

    /**
     * Bottom sheet title.
     * Usually same as cardProblem.
     */
    private String detailTitle;

    /**
     * Bottom sheet block 1.
     */
    private String legacyIssueLabel;
    private String legacyIssueTitle;
    private String legacyIssueDescription;

    /**
     * Bottom sheet block 2.
     */
    private String solutionLabel;
    private String solutionTitle;
    private String solutionDescription;

    /**
     * Performance metric.
     * Example:
     * metricValue = 87%
     * metricLabel = Resolved < 48hrs
     */
    private String metricValue;
    private String metricLabel;

    /**
     * Optional explanation behind the metric.
     */
    private String resultTitle;
    private String resultDescription;

    /**
     * Implementation date.
     */
    private LocalDate implementedAt;
    private String implementedMonthYear;
    private String implementationLabel;

    private String impactLevel;
    private String evidenceStatus;

    private Integer displayOrder;
}