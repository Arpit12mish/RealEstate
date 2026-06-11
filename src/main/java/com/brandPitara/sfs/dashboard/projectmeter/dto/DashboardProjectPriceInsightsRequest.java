package com.brandPitara.sfs.dashboard.projectmeter.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DashboardProjectPriceInsightsRequest {

    @Min(value = 0, message = "launchPrice must be 0 or greater")
    private Long launchPrice;

    @Min(value = 0, message = "currentPrice must be 0 or greater")
    private Long currentPrice;

    @Min(value = 0, message = "averageAreaPrice must be 0 or greater")
    private Long averageAreaPrice;
}
