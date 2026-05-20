package com.brandPitara.sfs.dashboard.scraping.dto;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

/**
 * Candidate project data extracted from a RERA portal scrape.
 * Fields align with ProjectUpsertRequest; cityName is used instead of cityId
 * since the numeric city ID is resolved during the import review step, not at scrape time.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScrapedProjectCandidateDto {

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
    private ProjectStatus status;
    private Set<PropertyType> propertyTypes;
}
