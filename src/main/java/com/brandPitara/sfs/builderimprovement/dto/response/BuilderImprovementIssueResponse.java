package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementIssueResponse {

    private Long id;

    private String issueType;
    private String title;
    private String description;
    private String shortDescription;

    private String status;
    private String statusLabel;

    private OffsetDateTime reportedAt;
    private OffsetDateTime resolvedAt;
    private LocalDate targetResolutionDate;

    /**
     * Used in Issue History bottom sheet.
     * Example: Jan 29 / Oct 12 / Nov 05
     */
    private String dateLabel;

    /**
     * Used in Resolved tab.
     * Example: Oct 12
     */
    private String resolvedDateLabel;

    /**
     * Used in In Progress tab.
     * Example: Nov 05
     */
    private String targetDateLabel;

    /**
     * UI label:
     * Resolved tab -> Action Taken
     * In Progress tab -> Current action / action pending
     */
    private String actionText;

    /**
     * UI label:
     * In Progress tab -> Why it is still in progress
     */
    private String delayReason;

    private String evidenceStatus;
    private Integer displayOrder;
}