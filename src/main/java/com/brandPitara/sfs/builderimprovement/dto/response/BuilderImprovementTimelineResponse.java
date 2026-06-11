package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementTimelineResponse {

    private Long id;

    private String timelineType;
    private String iconKey;

    /**
     * Raw event date for sorting/details.
     */
    private LocalDate eventDate;

    /**
     * UI list label.
     * Example: JAN 2025
     */
    private String monthLabel;

    /**
     * UI bottom sheet header.
     * Example: Jan 2025 • Delay Observations
     */
    private String detailHeader;

    /**
     * UI list title.
     * Example: Delay concerns observed
     */
    private String title;

    private String shortDescription;

    /**
     * Bottom sheet block 1.
     */
    private String problemLabel;
    private String problemObserved;

    /**
     * Bottom sheet block 2.
     */
    private String actionLabel;
    private String actionTaken;

    /**
     * Bottom sheet block 3.
     */
    private String currentStatusLabel;
    private String currentStatus;

    private String evidenceStatus;
    private Integer displayOrder;
}