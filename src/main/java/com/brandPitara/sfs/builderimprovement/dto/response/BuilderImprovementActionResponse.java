package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementActionResponse {

    private Long id;

    private String actionType;
    private String title;
    private String subtitle;

    /**
     * UI label: Problem
     * DB source: context_text
     */
    private String problem;

    /**
     * UI label: Action
     * DB source: action_text
     */
    private String actionTaken;

    /**
     * UI label: Result
     * DB source: result_text
     */
    private String result;

    private String iconKey;
    private String impactLevel;
    private String evidenceStatus;
    private Integer displayOrder;
}