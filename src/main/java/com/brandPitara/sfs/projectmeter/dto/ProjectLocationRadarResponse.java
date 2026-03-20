package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLocationRadarResponse {
    private Double metroScore;
    private Double educationScore;
    private Double healthcareScore;
    private Double retailScore;
    private Double jobScore;
    private Double leisureScore;
    private Double currentStrengthScore;
    private Double futureGrowthScore;
    private Double finalScore;
    private Double appreciationPercent3Y;
    private String scoreSummary;
    private Boolean verified;
}