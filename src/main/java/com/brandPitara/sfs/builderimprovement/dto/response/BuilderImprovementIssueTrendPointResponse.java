package com.brandPitara.sfs.builderimprovement.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderImprovementIssueTrendPointResponse {

    private String monthLabel;

    /**
     * Number of issues reported in this month.
     */
    private Long reportedCount;

    /**
     * Number of issues resolved in this month.
     */
    private Long resolvedCount;

    /**
     * Number of open issues at the end of this month.
     * Useful for downward trend graph.
     */
    private Long openAtMonthEndCount;
}