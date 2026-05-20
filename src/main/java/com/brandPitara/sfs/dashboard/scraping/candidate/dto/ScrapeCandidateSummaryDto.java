package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.brandPitara.sfs.dashboard.scraping.enums.ScrapeFieldConfidenceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ScrapeCandidateSummaryDto {

    private Long id;
    private ReraSourceCode sourceCode;
    private String reraNumber;
    private String projectName;
    private String builderName;
    private boolean found;
    private Integer confidenceScore;
    private ScrapeFieldConfidenceStatus confidenceStatus;
    private Integer foundFields;
    private Integer missingFields;
    private ScrapeCandidateStatus status;
    private Long linkedBuilderId;
    private Long linkedProjectId;
    private Long appliedProjectId;
    private String sourceDetailUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
