package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapeCandidateLandUtilizationDto {

    private Long id;
    private Double totalLandAreaSqm;
    private Double residentialAreaSqm;
    private Double commercialAreaSqm;
    private Double parksAreaSqm;
    private Double openAreaSqm;
    private Double parkingAreaSqm;
    private Double utilityAreaSqm;
    private String sourceLabel;
    private String remarks;
    private boolean verified;
}
