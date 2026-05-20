package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.brandPitara.sfs.dashboard.scraping.enums.ScrapeFieldConfidenceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class ScrapeCandidateDetailResponse {

    // ── Core candidate fields ────────────────────────────────────────────────
    private Long id;
    private ReraSourceCode sourceCode;
    private String reraNumber;
    private boolean found;
    private boolean captchaDetected;
    private String sourceSearchUrl;
    private String sourceDetailUrl;
    private String finalUrl;
    private String pageTitle;
    private String rawHtmlPath;
    private String screenshotPath;
    private Integer confidenceScore;
    private ScrapeFieldConfidenceStatus confidenceStatus;
    private Integer totalExpectedFields;
    private Integer foundFields;
    private Integer missingFields;
    private ScrapeCandidateStatus status;
    private Long linkedBuilderId;
    private Long linkedProjectId;
    private Long appliedProjectId;
    private String remarks;
    private String captchaSessionId;
    private OffsetDateTime captchaExpiresAt;
    private OffsetDateTime appliedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // ── Mapped candidate data ────────────────────────────────────────────────
    private ScrapeCandidateProjectDto projectCandidate;
    private ScrapeCandidateBuilderDto builderCandidate;
    private List<ScrapeCandidateComplianceItemDto> complianceCandidates;
    private ScrapeCandidateCostBreakdownDto costBreakdownCandidate;
    private ScrapeCandidateLandUtilizationDto landUtilizationCandidate;
    private List<ScrapeCandidateDocumentDto> documentCandidates;

    // ── Evidence / audit ────────────────────────────────────────────────────
    private List<ScrapeCandidateFieldResultDto> fieldResults;
    private List<ScrapeCandidateFieldResultDto> missingFieldResults;
    private List<ScrapeCandidateRawValueDto> rawValues;
}
