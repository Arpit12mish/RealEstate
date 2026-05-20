package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapeCandidateBuilderDto {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String addressLine;
    private String cityName;
    private String website;
}
