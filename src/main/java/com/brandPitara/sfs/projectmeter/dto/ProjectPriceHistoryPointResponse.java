package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPriceHistoryPointResponse {
    private String yearLabel;
    private Long projectPrice;
    private Long averageAreaPrice;
}