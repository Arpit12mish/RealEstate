package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLandUtilizationResponse {
    private Double totalLandAreaSqm;
    private Double commercialAreaSqm;
    private Double parksAreaSqm;
    private Double openAreaSqm;
    private Double residentialAreaSqm;
    private Double parkingAreaSqm;
    private Double utilityAreaSqm;
}