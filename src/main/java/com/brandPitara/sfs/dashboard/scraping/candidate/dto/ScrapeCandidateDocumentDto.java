package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateDocumentType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapeCandidateDocumentDto {

    private Long id;
    private ScrapeCandidateDocumentType documentType;
    private String title;
    private String documentUrl;
    private String sourceLabel;
}
