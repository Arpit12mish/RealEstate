package com.brandPitara.sfs.dashboard.scraping.candidate.service.impl;

import com.brandPitara.sfs.dashboard.scraping.candidate.dto.*;
import com.brandPitara.sfs.dashboard.scraping.candidate.entity.*;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.candidate.mapper.ScrapeCandidateEntityMapper;
import com.brandPitara.sfs.dashboard.scraping.candidate.mapper.ScrapeCandidateEntityMapper.CandidateEntityGraph;
import com.brandPitara.sfs.dashboard.scraping.candidate.repository.*;
import com.brandPitara.sfs.dashboard.scraping.candidate.service.ScrapeCandidateService;
import com.brandPitara.sfs.dashboard.scraping.candidate.service.ScrapeCandidateValidationService;
import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchRequest;
import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchResponse;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.brandPitara.sfs.dashboard.scraping.service.ReraNumberSearchService;
import com.brandPitara.sfs.dashboard.scraping.session.ScrapeSessionStore;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapeCandidateServiceImpl implements ScrapeCandidateService {

    private final ReraNumberSearchService   reraNumberSearchService;
    private final ScrapeCandidateEntityMapper entityMapper;
    private final ScrapeCandidateValidationService validationService;
    private final DashboardCurrentUserService currentUserService;
    private final ScrapeSessionStore        sessionStore;

    private final ScrapeCandidateRepository            candidateRepository;
    private final ScrapeCandidateProjectRepository     projectRepository;
    private final ScrapeCandidateBuilderRepository     builderRepository;
    private final ScrapeCandidateComplianceItemRepository complianceRepository;
    private final ScrapeCandidateFieldResultRepository  fieldResultRepository;
    private final ScrapeCandidateRawValueRepository    rawValueRepository;
    private final ScrapeCandidateCostBreakdownRepository costBreakdownRepository;
    private final ScrapeCandidateLandUtilizationRepository landUtilizationRepository;
    private final ScrapeCandidateDocumentRepository    documentRepository;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ScrapeCandidateDetailResponse save(SaveScrapeCandidateRequest request) {
        ReraNumberSearchRequest searchRequest = new ReraNumberSearchRequest();
        searchRequest.setSourceCode(request.getSourceCode());
        searchRequest.setReraNumber(request.getReraNumber());
        searchRequest.setSaveEvidence(request.isSaveEvidence());
        searchRequest.setIncludeRaw(true);
        searchRequest.setCaptchaText(request.getCaptchaText());

        log.info("ScrapeCandidateService.save: sourceCode={} reraNumber={}",
                request.getSourceCode(), request.getReraNumber());

        ReraNumberSearchResponse response = reraNumberSearchService.searchByReraNumber(searchRequest);

        Long userId = tryGetCurrentUserId();
        CandidateEntityGraph graph = entityMapper.toEntityGraph(response, userId);

        DashboardScrapeCandidateEntity saved = candidateRepository.save(graph.candidate());
        persistChildren(saved, graph);

        // Link the live session to the persisted candidate so refresh-captcha can look it up
        if (saved.getCaptchaSessionId() != null) {
            sessionStore.updateCandidateId(saved.getCaptchaSessionId(), saved.getId());
        }

        log.info("ScrapeCandidateService.save: persisted candidateId={} status={}",
                saved.getId(), saved.getStatus());

        return buildDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScrapeCandidateDetailResponse getById(Long candidateId) {
        DashboardScrapeCandidateEntity candidate = loadOrThrow(candidateId);
        return buildDetailResponse(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScrapeCandidateSummaryDto> list(ScrapeCandidateStatus status,
                                                 ReraSourceCode sourceCode,
                                                 String reraNumber,
                                                 Pageable pageable) {
        Page<DashboardScrapeCandidateEntity> page =
                candidateRepository.findAll(
                        ScrapeCandidateSpecifications.filter(status, sourceCode, reraNumber),
                        pageable);

        List<Long> ids = page.map(DashboardScrapeCandidateEntity::getId).toList();

        Map<Long, String> projectNames = projectRepository.findByCandidateIdIn(ids).stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.toMap(
                        p -> p.getCandidate().getId(),
                        DashboardScrapeCandidateProjectEntity::getName,
                        (a, b) -> a));

        Map<Long, String> builderNames = builderRepository.findByCandidateIdIn(ids).stream()
                .filter(b -> b.getName() != null)
                .collect(Collectors.toMap(
                        b -> b.getCandidate().getId(),
                        DashboardScrapeCandidateBuilderEntity::getName,
                        (a, b) -> a));

        return page.map(c -> ScrapeCandidateSummaryDto.builder()
                .id(c.getId())
                .sourceCode(c.getSourceCode())
                .reraNumber(c.getReraNumber())
                .projectName(projectNames.getOrDefault(c.getId(), null))
                .builderName(builderNames.getOrDefault(c.getId(), null))
                .found(c.isFound())
                .confidenceScore(c.getConfidenceScore())
                .confidenceStatus(c.getConfidenceStatus())
                .foundFields(c.getFoundFields())
                .missingFields(c.getMissingFields())
                .status(c.getStatus())
                .linkedBuilderId(c.getLinkedBuilderId())
                .linkedProjectId(c.getLinkedProjectId())
                .appliedProjectId(c.getAppliedProjectId())
                .sourceDetailUrl(c.getSourceDetailUrl())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build());
    }

    @Override
    @Transactional
    public ScrapeCandidateDetailResponse linkBuilder(Long candidateId, LinkBuilderRequest request) {
        DashboardScrapeCandidateEntity candidate = loadOrThrow(candidateId);
        candidate.setLinkedBuilderId(request.getBuilderId());
        candidateRepository.save(candidate);
        log.info("ScrapeCandidateService.linkBuilder: candidateId={} builderId={}",
                candidateId, request.getBuilderId());
        return buildDetailResponse(candidate);
    }

    @Override
    @Transactional
    public ScrapeCandidateDetailResponse updateStatus(Long candidateId,
                                                       UpdateCandidateStatusRequest request) {
        DashboardScrapeCandidateEntity candidate = loadOrThrow(candidateId);

        validationService.validateStatusTransition(candidate, request);

        candidate.setStatus(request.getStatus());
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            candidate.setRemarks(request.getRemarks());
        }

        candidateRepository.save(candidate);
        log.info("ScrapeCandidateService.updateStatus: candidateId={} status={}",
                candidateId, request.getStatus());
        return buildDetailResponse(candidate);
    }

    @Override
    @Transactional
    public ScrapeCandidateDetailResponse submitCaptcha(Long candidateId, SubmitCaptchaRequest request) {
        DashboardScrapeCandidateEntity candidate = loadOrThrow(candidateId);

        if (candidate.getStatus() != ScrapeCandidateStatus.CAPTCHA_REQUIRED
                && candidate.getStatus() != ScrapeCandidateStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Candidate " + candidateId + " has status " + candidate.getStatus() +
                    " and does not require captcha submission. " +
                    "Only CAPTCHA_REQUIRED or FAILED candidates can be retried.");
        }

        // Prefer the session ID supplied by the caller; fall back to what was stored on the candidate
        String captchaSessionId = request.getCaptchaSessionId() != null
                ? request.getCaptchaSessionId()
                : candidate.getCaptchaSessionId();

        ReraNumberSearchRequest searchRequest = new ReraNumberSearchRequest();
        searchRequest.setSourceCode(candidate.getSourceCode());
        searchRequest.setReraNumber(candidate.getReraNumber());
        searchRequest.setSaveEvidence(true);
        searchRequest.setIncludeRaw(true);
        searchRequest.setCaptchaSessionId(captchaSessionId);
        searchRequest.setCaptchaText(request.getCaptchaText());

        log.info("ScrapeCandidateService.submitCaptcha: retrying candidateId={} sourceCode={} reraNumber={}",
                candidateId, candidate.getSourceCode(), candidate.getReraNumber());

        ReraNumberSearchResponse response = reraNumberSearchService.searchByReraNumber(searchRequest);

        CandidateEntityGraph graph = entityMapper.toEntityGraph(response, candidate.getCreatedByDashboardUserId());

        deleteAllChildren(candidateId);

        applyResponseToCandidate(candidate, graph.candidate());
        candidateRepository.save(candidate);

        persistChildren(candidate, graph);

        log.info("ScrapeCandidateService.submitCaptcha: updated candidateId={} status={}",
                candidateId, candidate.getStatus());

        return buildDetailResponse(candidate);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Package-visible: used by ScrapeCandidateApplyServiceImpl
    // ─────────────────────────────────────────────────────────────────────────

    DashboardScrapeCandidateEntity loadOrThrow(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new NotFoundException("Scrape candidate not found: " + candidateId));
    }

    ScrapeCandidateDetailResponse buildDetailResponse(DashboardScrapeCandidateEntity candidate) {
        Long cid = candidate.getId();

        Optional<DashboardScrapeCandidateProjectEntity> projectOpt =
                projectRepository.findByCandidateId(cid);
        Optional<DashboardScrapeCandidateBuilderEntity> builderOpt =
                builderRepository.findByCandidateId(cid);
        List<DashboardScrapeCandidateComplianceItemEntity> complianceItems =
                complianceRepository.findByCandidateIdOrderByDisplayOrderAscIdAsc(cid);
        List<DashboardScrapeCandidateFieldResultEntity> allFieldResults =
                fieldResultRepository.findByCandidateIdOrderByFoundDescFieldKeyAsc(cid);
        List<DashboardScrapeCandidateRawValueEntity> rawValues =
                rawValueRepository.findByCandidateIdOrderByRawKeyAsc(cid);
        Optional<DashboardScrapeCandidateCostBreakdownEntity> costBreakdownOpt =
                costBreakdownRepository.findByCandidateId(cid);
        Optional<DashboardScrapeCandidateLandUtilizationEntity> landOpt =
                landUtilizationRepository.findByCandidateId(cid);
        List<DashboardScrapeCandidateDocumentEntity> documents =
                documentRepository.findByCandidateIdOrderByDocumentTypeAscIdAsc(cid);

        List<ScrapeCandidateFieldResultDto> foundResults = allFieldResults.stream()
                .filter(DashboardScrapeCandidateFieldResultEntity::isFound)
                .map(entityMapper::toFieldResultDto)
                .toList();

        List<ScrapeCandidateFieldResultDto> missingResults = allFieldResults.stream()
                .filter(f -> !f.isFound())
                .map(entityMapper::toFieldResultDto)
                .toList();

        // Resolve captchaExpiresAt from the live session store (not persisted in DB)
        OffsetDateTime captchaExpiresAt = candidate.getCaptchaSessionId() != null
                ? sessionStore.get(candidate.getCaptchaSessionId())
                        .map(s -> s.getExpiresAt())
                        .orElse(null)
                : null;

        return ScrapeCandidateDetailResponse.builder()
                .id(candidate.getId())
                .sourceCode(candidate.getSourceCode())
                .reraNumber(candidate.getReraNumber())
                .found(candidate.isFound())
                .captchaDetected(candidate.isCaptchaDetected())
                .sourceSearchUrl(candidate.getSourceSearchUrl())
                .sourceDetailUrl(candidate.getSourceDetailUrl())
                .finalUrl(candidate.getFinalUrl())
                .pageTitle(candidate.getPageTitle())
                .rawHtmlPath(candidate.getRawHtmlPath())
                .screenshotPath(candidate.getScreenshotPath())
                .confidenceScore(candidate.getConfidenceScore())
                .confidenceStatus(candidate.getConfidenceStatus())
                .totalExpectedFields(candidate.getTotalExpectedFields())
                .foundFields(candidate.getFoundFields())
                .missingFields(candidate.getMissingFields())
                .status(candidate.getStatus())
                .linkedBuilderId(candidate.getLinkedBuilderId())
                .linkedProjectId(candidate.getLinkedProjectId())
                .appliedProjectId(candidate.getAppliedProjectId())
                .remarks(candidate.getRemarks())
                .captchaSessionId(candidate.getCaptchaSessionId())
                .captchaExpiresAt(captchaExpiresAt)
                .appliedAt(candidate.getAppliedAt())
                .createdAt(candidate.getCreatedAt())
                .updatedAt(candidate.getUpdatedAt())
                .projectCandidate(projectOpt.map(entityMapper::toProjectDto).orElse(null))
                .builderCandidate(builderOpt.map(entityMapper::toBuilderDto).orElse(null))
                .complianceCandidates(complianceItems.stream()
                        .map(entityMapper::toComplianceDto).toList())
                .costBreakdownCandidate(costBreakdownOpt.map(entityMapper::toCostBreakdownDto).orElse(null))
                .landUtilizationCandidate(landOpt.map(entityMapper::toLandUtilizationDto).orElse(null))
                .documentCandidates(documents.stream().map(entityMapper::toDocumentDto).toList())
                .fieldResults(foundResults)
                .missingFieldResults(missingResults)
                .rawValues(rawValues.stream().map(entityMapper::toRawValueDto).toList())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void persistChildren(DashboardScrapeCandidateEntity saved, CandidateEntityGraph graph) {
        if (graph.project() != null) {
            graph.project().setCandidate(saved);
            projectRepository.save(graph.project());
        }
        if (graph.builder() != null) {
            graph.builder().setCandidate(saved);
            builderRepository.save(graph.builder());
        }
        if (!graph.complianceItems().isEmpty()) {
            graph.complianceItems().forEach(c -> c.setCandidate(saved));
            complianceRepository.saveAll(graph.complianceItems());
        }
        if (!graph.fieldResults().isEmpty()) {
            graph.fieldResults().forEach(f -> f.setCandidate(saved));
            fieldResultRepository.saveAll(graph.fieldResults());
        }
        if (!graph.rawValues().isEmpty()) {
            graph.rawValues().forEach(r -> r.setCandidate(saved));
            rawValueRepository.saveAll(graph.rawValues());
        }
        if (graph.costBreakdown() != null) {
            graph.costBreakdown().setCandidate(saved);
            costBreakdownRepository.save(graph.costBreakdown());
        }
        if (graph.landUtilization() != null) {
            graph.landUtilization().setCandidate(saved);
            landUtilizationRepository.save(graph.landUtilization());
        }
        if (!graph.documents().isEmpty()) {
            graph.documents().forEach(d -> d.setCandidate(saved));
            documentRepository.saveAll(graph.documents());
        }
    }

    private void deleteAllChildren(Long candidateId) {
        projectRepository.deleteByCandidateId(candidateId);
        builderRepository.deleteByCandidateId(candidateId);
        complianceRepository.deleteByCandidateId(candidateId);
        fieldResultRepository.deleteByCandidateId(candidateId);
        rawValueRepository.deleteByCandidateId(candidateId);
        costBreakdownRepository.deleteByCandidateId(candidateId);
        landUtilizationRepository.deleteByCandidateId(candidateId);
        documentRepository.deleteByCandidateId(candidateId);
    }

    private void applyResponseToCandidate(DashboardScrapeCandidateEntity existing,
                                           DashboardScrapeCandidateEntity newData) {
        existing.setFound(newData.isFound());
        existing.setCaptchaDetected(newData.isCaptchaDetected());
        existing.setSourceSearchUrl(newData.getSourceSearchUrl());
        existing.setSourceDetailUrl(newData.getSourceDetailUrl());
        existing.setFinalUrl(newData.getFinalUrl());
        existing.setPageTitle(newData.getPageTitle());
        existing.setRawHtmlPath(newData.getRawHtmlPath());
        existing.setScreenshotPath(newData.getScreenshotPath());
        existing.setConfidenceScore(newData.getConfidenceScore());
        existing.setConfidenceStatus(newData.getConfidenceStatus());
        existing.setTotalExpectedFields(newData.getTotalExpectedFields());
        existing.setFoundFields(newData.getFoundFields());
        existing.setMissingFields(newData.getMissingFields());
        existing.setStatus(newData.getStatus());
        if (newData.getStatus() == ScrapeCandidateStatus.CAPTCHA_REQUIRED) {
            existing.setCaptchaSessionId(newData.getCaptchaSessionId());
            existing.setRemarks("CAPTCHA required again. Wrong or expired captcha. Please retry with the correct text.");
        } else {
            existing.setCaptchaSessionId(null);
            existing.setRemarks(null);
        }
    }

    @Override
    @Transactional
    public ScrapeCandidateDetailResponse refreshCaptcha(Long candidateId) {
        DashboardScrapeCandidateEntity existing = loadOrThrow(candidateId);

        if (existing.getStatus() != ScrapeCandidateStatus.CAPTCHA_REQUIRED
                && existing.getStatus() != ScrapeCandidateStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Captcha can be refreshed only for CAPTCHA_REQUIRED or FAILED candidates. Current status: "
                    + existing.getStatus());
        }

        ReraNumberSearchRequest searchRequest = new ReraNumberSearchRequest();
        searchRequest.setSourceCode(existing.getSourceCode());
        searchRequest.setReraNumber(existing.getReraNumber());
        searchRequest.setSaveEvidence(true);
        searchRequest.setIncludeRaw(true);
        searchRequest.setCaptchaText(null);

        log.info("ScrapeCandidateService.refreshCaptcha: re-scraping candidateId={} to get fresh CAPTCHA",
                candidateId);

        ReraNumberSearchResponse response = reraNumberSearchService.searchByReraNumber(searchRequest);

        CandidateEntityGraph graph = entityMapper.toEntityGraph(response, existing.getCreatedByDashboardUserId());

        deleteAllChildren(existing.getId());
        applyResponseToCandidate(existing, graph.candidate());

        DashboardScrapeCandidateEntity saved = candidateRepository.save(existing);
        persistChildren(saved, graph);

        if (saved.getCaptchaSessionId() != null) {
            sessionStore.updateCandidateId(saved.getCaptchaSessionId(), saved.getId());
        }

        log.info("ScrapeCandidateService.refreshCaptcha: done candidateId={} status={}",
                saved.getId(), saved.getStatus());

        return buildDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getScreenshot(Long candidateId) {
        DashboardScrapeCandidateEntity candidate = loadOrThrow(candidateId);

        if (candidate.getScreenshotPath() == null || candidate.getScreenshotPath().isBlank()) {
            throw new NotFoundException("Screenshot not found for candidate: " + candidateId);
        }

        try {
            Path path = Paths.get(candidate.getScreenshotPath());

            if (!Files.exists(path)) {
                throw new NotFoundException(
                        "Screenshot file does not exist on disk: " + candidate.getScreenshotPath());
            }

            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.noCache())
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid screenshot path for candidate " + candidateId, e);
        }
    }

    private Long tryGetCurrentUserId() {
        try {
            return currentUserService.getCurrentUserOrThrow().getId();
        } catch (Exception e) {
            return null;
        }
    }
}
