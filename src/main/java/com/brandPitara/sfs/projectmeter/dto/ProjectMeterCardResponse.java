package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterCardResponse {
    private Long projectId;
    private String projectName;
    private String projectSlug;

    private Long builderId;
    private String builderName;
    private String builderLogoUrl;

    private String coverImageUrl;

    private String addressLine;
    private String cityName;

    private Long priceMin;
    private Long priceMax;

    private Integer constructionProgressPercent;
    private Double appreciationPercent;
    private LocalDate constructionStartDate;

    private String timelineStatus;
    private Integer delayDays;
}