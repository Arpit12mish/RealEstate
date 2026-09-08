package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterProjectResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;

    private Long builderId;
    private String builderName;
    private String builderLogoUrl;

    private Long cityId;
    private String cityName;
    private String addressLine;
    private Double latitude;
    private Double longitude;

    private Long priceMin;
    private Long priceMax;
    private Long monthlyEmiMin;
    private Long monthlyEmiMax;
    private Long averagePricePerSqft;

    private LocalDate startDate;
    private LocalDate possessionDate;
    private String reraNumber;
    private ProjectStatus status;
    private Set<PropertyType> propertyTypes;

    private String brochureUrl;
    private Boolean isFavorite;
    private Long favoriteCount;
}
