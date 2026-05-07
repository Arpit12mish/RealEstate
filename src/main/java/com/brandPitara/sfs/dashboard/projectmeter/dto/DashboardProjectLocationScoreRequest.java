package com.brandPitara.sfs.dashboard.projectmeter.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardProjectLocationScoreRequest {

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double metroScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double educationScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double healthcareScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double retailScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double jobScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double leisureScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double currentStrengthScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double futureGrowthScore;

    @DecimalMin(value = "0.0") @DecimalMax(value = "10.0")
    private Double finalScore;

    @DecimalMin(value = "-100.0", message = "appreciationPercent3Y must be -100 or greater")
    @DecimalMax(value = "9999.0",  message = "appreciationPercent3Y must not exceed 9999")
    private Double appreciationPercent3Y;

    @Size(max = 1000)
    private String scoreSummary;
    private Boolean verified;
}