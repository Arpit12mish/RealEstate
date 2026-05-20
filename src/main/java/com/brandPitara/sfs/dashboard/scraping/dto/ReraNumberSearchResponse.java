package com.brandPitara.sfs.dashboard.scraping.dto;

import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReraNumberSearchResponse {

    private ReraSourceCode sourceCode;
    private String reraNumber;
    private boolean found;
    private boolean captchaDetected;
    private String captchaSessionId;
    private OffsetDateTime captchaExpiresAt;
    private OffsetDateTime requestedAt;
    private String sourceSearchUrl;
    private String sourceDetailUrl;
    private String finalUrl;
    private String title;
    private String rawHtmlPath;
    private String screenshotPath;
    private ReraSearchSummaryDto summary;
    private ScrapedProjectCandidateDto projectCandidate;
    private ScrapedBuilderCandidateDto builderCandidate;
    private List<ScrapedComplianceCandidateDto> complianceCandidates;
    private List<ScrapeFieldResultDto> fieldResults;
    private List<ScrapeMissingFieldDto> missingFields;
    private List<String> warnings;
    private Map<String, Object> raw;
}
