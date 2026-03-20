package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCostBreakdownResponse {
    private Long totalCost;
    private Long landCost;
    private Long constructionCost;
    private Long infrastructureCost;
    private Long otherCost;
    private String sourceLabel;
    private String remarks;
    private Boolean verified;
}