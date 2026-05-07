package com.brandPitara.sfs.dashboard.projectmeter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardProjectCostBreakdownRequest {

    @Min(value = 0, message = "landCost must be 0 or greater")
    private Long landCost;

    @Min(value = 0, message = "constructionCost must be 0 or greater")
    private Long constructionCost;

    @Min(value = 0, message = "infrastructureCost must be 0 or greater")
    private Long infrastructureCost;

    @Min(value = 0, message = "otherCost must be 0 or greater")
    private Long otherCost;

    @Min(value = 0, message = "totalCost must be 0 or greater")
    private Long totalCost;

    @Size(max = 180)
    private String sourceLabel;

    @Size(max = 500)
    private String remarks;
    private Boolean verified;
}