package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPriceHistoryPointResponse {
    private Long id;
    private String yearLabel;
    private Long projectPrice;
    private Long averageAreaPrice;
    private Integer displayOrder;
    private Boolean verified;
}