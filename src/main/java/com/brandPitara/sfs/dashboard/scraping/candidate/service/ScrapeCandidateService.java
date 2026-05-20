package com.brandPitara.sfs.dashboard.scraping.candidate.service;

import com.brandPitara.sfs.dashboard.scraping.candidate.dto.*;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface ScrapeCandidateService {

    ScrapeCandidateDetailResponse save(SaveScrapeCandidateRequest request);

    ScrapeCandidateDetailResponse getById(Long candidateId);

    Page<ScrapeCandidateSummaryDto> list(ScrapeCandidateStatus status,
                                         ReraSourceCode sourceCode,
                                         String reraNumber,
                                         Pageable pageable);

    ScrapeCandidateDetailResponse linkBuilder(Long candidateId, LinkBuilderRequest request);

    ScrapeCandidateDetailResponse updateStatus(Long candidateId, UpdateCandidateStatusRequest request);

    ScrapeCandidateDetailResponse submitCaptcha(Long candidateId, SubmitCaptchaRequest request);

    ScrapeCandidateDetailResponse refreshCaptcha(Long candidateId);

    ResponseEntity<Resource> getScreenshot(Long candidateId);
}
