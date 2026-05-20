package com.brandPitara.sfs.dashboard.scraping.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Candidate builder/promoter data extracted from a RERA portal scrape.
 * Fields align with BuilderUpsertRequest; cityName is used instead of cityId.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScrapedBuilderCandidateDto {

    private String name;
    private String description;
    private String phone;
    private String whatsapp;
    private String email;
    private String addressLine;
    private String cityName;
    private Double latitude;
    private Double longitude;
    private String logoUrl;
}
