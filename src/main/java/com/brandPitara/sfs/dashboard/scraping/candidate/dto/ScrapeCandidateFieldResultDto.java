package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapeCandidateFieldResultDto {

    private Long id;
    private String section;
    private String fieldKey;
    private String fieldLabel;
    private boolean found;
    private String valueText;
    private String sourceLabel;
    private Integer confidence;
    private String reason;
}
