package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapeCandidateRawValueDto {

    private Long id;
    private String rawKey;
    private String rawValue;
}
