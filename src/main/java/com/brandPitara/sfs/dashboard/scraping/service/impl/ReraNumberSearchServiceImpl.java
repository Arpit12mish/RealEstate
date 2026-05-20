package com.brandPitara.sfs.dashboard.scraping.service.impl;

import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchRequest;
import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchResponse;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.brandPitara.sfs.dashboard.scraping.mapper.ReraProjectCandidateMapper;
import com.brandPitara.sfs.dashboard.scraping.service.ReraNumberSearchService;
import com.brandPitara.sfs.dashboard.scraping.source.CaptchaCapableScraper;
import com.brandPitara.sfs.dashboard.scraping.source.ReraRawSearchResult;
import com.brandPitara.sfs.dashboard.scraping.source.ReraSourceScraper;
import com.brandPitara.sfs.dashboard.scraping.util.SavedScrapeFile;
import com.brandPitara.sfs.dashboard.scraping.util.ScrapeFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReraNumberSearchServiceImpl implements ReraNumberSearchService {

    private static final DateTimeFormatter PREFIX_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final List<ReraSourceScraper> scrapers;
    private final ReraProjectCandidateMapper candidateMapper;
    private final ScrapeFileStorageService fileStorageService;

    @Override
    public ReraNumberSearchResponse searchByReraNumber(ReraNumberSearchRequest request) {
        ReraSourceScraper scraper = findScraper(request.getSourceCode());

        log.info("RERA search started: sourceCode={} reraNumber={} sessionId={}",
                request.getSourceCode(), request.getReraNumber(), request.getCaptchaSessionId());

        ReraRawSearchResult rawResult;

        // If a live session ID is provided, resume the existing browser session
        if (request.getCaptchaSessionId() != null
                && scraper instanceof CaptchaCapableScraper captchaScraper) {
            rawResult = captchaScraper.continueWithCaptcha(
                    request.getCaptchaSessionId(), request.getCaptchaText());
        } else {
            rawResult = scraper.searchByReraNumber(request.getReraNumber(), request.getCaptchaText());
        }

        String sessionPrefix = generateSessionPrefix(request);
        String rawHtmlPath    = null;
        String screenshotPath = null;

        if (request.isSaveEvidence()) {
            rawHtmlPath    = saveHtml(rawResult, sessionPrefix);
            screenshotPath = saveScreenshot(rawResult, sessionPrefix);
        }

        ReraNumberSearchResponse response = candidateMapper.map(
                rawResult,
                request.getReraNumber(),
                request.getSourceCode(),
                request.isIncludeRaw()
        );

        response.setRawHtmlPath(rawHtmlPath);
        response.setScreenshotPath(screenshotPath);
        response.setCaptchaSessionId(rawResult.getCaptchaSessionId());
        response.setCaptchaExpiresAt(rawResult.getCaptchaExpiresAt());

        log.info("RERA search complete: sourceCode={} reraNumber={} found={} confidence={} captcha={}",
                request.getSourceCode(), request.getReraNumber(),
                response.isFound(),
                response.getSummary() != null ? response.getSummary().getConfidenceScore() : "-",
                response.isCaptchaDetected());

        return response;
    }

    @Override
    public byte[] refreshCaptchaScreenshot(ReraSourceCode sourceCode, String sessionId,
                                            List<String> warnings) {
        ReraSourceScraper scraper = findScraper(sourceCode);
        if (!(scraper instanceof CaptchaCapableScraper captchaScraper)) {
            warnings.add("Source " + sourceCode + " does not support captcha refresh.");
            return null;
        }
        return captchaScraper.refreshCaptchaScreenshot(sessionId, warnings);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private ReraSourceScraper findScraper(ReraSourceCode sourceCode) {
        return scrapers.stream()
                .filter(s -> s.supports(sourceCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source not implemented yet: " + sourceCode));
    }

    private String saveHtml(ReraRawSearchResult rawResult, String prefix) {
        if (rawResult.getHtml() == null || rawResult.getHtml().isBlank()) return null;
        try {
            SavedScrapeFile saved = fileStorageService.saveHtml(prefix, rawResult.getHtml());
            return saved.path();
        } catch (Exception e) {
            log.warn("Failed to save scrape HTML for prefix={}: {}", prefix, e.getMessage());
            rawResult.getWarnings().add("Failed to save raw HTML: " + e.getMessage());
            return null;
        }
    }

    private String saveScreenshot(ReraRawSearchResult rawResult, String prefix) {
        if (rawResult.getScreenshotBytes() == null || rawResult.getScreenshotBytes().length == 0) return null;
        try {
            SavedScrapeFile saved = fileStorageService.saveScreenshot(prefix, rawResult.getScreenshotBytes());
            return saved.path();
        } catch (Exception e) {
            log.warn("Failed to save scrape screenshot for prefix={}: {}", prefix, e.getMessage());
            rawResult.getWarnings().add("Failed to save screenshot: " + e.getMessage());
            return null;
        }
    }

    private String generateSessionPrefix(ReraNumberSearchRequest request) {
        String timestamp = LocalDateTime.now().format(PREFIX_FORMATTER);
        String shortId   = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String safeCode  = request.getSourceCode().name().toLowerCase();
        return "rera-" + safeCode + "-" + timestamp + "-" + shortId;
    }
}
