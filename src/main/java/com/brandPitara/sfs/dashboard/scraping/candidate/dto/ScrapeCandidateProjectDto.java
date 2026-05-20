package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ScrapeCandidateProjectDto {

    private Long id;
    private String name;
    private String description;
    private String cityName;
    private String addressLine;
    private Double latitude;
    private Double longitude;
    private Long priceMin;
    private Long priceMax;
    private LocalDate possessionDate;
    private String reraNumber;
    private String projectStatus;
    private List<String> propertyTypes;
}
