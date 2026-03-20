package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPriceInsightsResponse {
    private Long launchPrice;
    private Long currentPrice;
    private Double appreciationPercent;
    private Long averageAreaPrice;
}