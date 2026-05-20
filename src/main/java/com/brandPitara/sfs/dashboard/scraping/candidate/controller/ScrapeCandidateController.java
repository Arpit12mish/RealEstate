package com.brandPitara.sfs.dashboard.scraping.candidate.controller;

import com.brandPitara.sfs.dashboard.scraping.candidate.dto.*;
import com.brandPitara.sfs.dashboard.scraping.candidate.enums.ScrapeCandidateStatus;
import com.brandPitara.sfs.dashboard.scraping.candidate.service.ScrapeCandidateApplyService;
import com.brandPitara.sfs.dashboard.scraping.candidate.service.ScrapeCandidateService;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/scraping/candidates")
@RequiredArgsConstructor
public class ScrapeCandidateController {

    private final ScrapeCandidateService      candidateService;
    private final ScrapeCandidateApplyService applyService;

    // ── Save (search + persist) ──────────────────────────────────────────────

    /**
     * POST /api/dashboard/scraping/candidates
     * Runs a live RERA scrape and saves the result as a new candidate record.
     * The scrape response is always persisted — including FAILED results.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapeCandidateDetailResponse> save(
            @Valid @RequestBody SaveScrapeCandidateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidateService.save(request));
    }

    // ── List ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/scraping/candidates
     * Paginated candidate list. All roles may read.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY','REVIEWER')")
    public ResponseEntity<Page<ScrapeCandidateSummaryDto>> list(
            @RequestParam(required = false) ScrapeCandidateStatus status,
            @RequestParam(required = false) ReraSourceCode sourceCode,
            @RequestParam(required = false) String reraNumber,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(candidateService.list(status, sourceCode, reraNumber, pageable));
    }

    // ── Detail ───────────────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/scraping/candidates/{id}
     * Full candidate detail including all child records. All roles may read.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY','REVIEWER')")
    public ResponseEntity<ScrapeCandidateDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.getById(id));
    }

    // ── Link builder ─────────────────────────────────────────────────────────

    /**
     * PATCH /api/dashboard/scraping/candidates/{id}/link-builder
     * Associates an existing builder with this candidate.
     */
    @PatchMapping("/{id}/link-builder")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public ResponseEntity<ScrapeCandidateDetailResponse> linkBuilder(
            @PathVariable Long id,
            @Valid @RequestBody LinkBuilderRequest request) {
        return ResponseEntity.ok(candidateService.linkBuilder(id, request));
    }

    // ── Status update ─────────────────────────────────────────────────────────

    /**
     * PATCH /api/dashboard/scraping/candidates/{id}/status
     * Changes the workflow status of a candidate.
     * Remarks are required when rejecting.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public ResponseEntity<ScrapeCandidateDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCandidateStatusRequest request) {
        return ResponseEntity.ok(candidateService.updateStatus(id, request));
    }

    // ── Submit captcha ────────────────────────────────────────────────────────

    /**
     * POST /api/dashboard/scraping/candidates/{id}/submit-captcha
     * Retries the scrape for a CAPTCHA_REQUIRED candidate using the admin-supplied captcha text.
     * All existing child records are replaced with the new scrape result.
     * The candidate is updated in-place — no new candidate is created.
     */
    @PostMapping("/{id}/submit-captcha")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public ResponseEntity<ScrapeCandidateDetailResponse> submitCaptcha(
            @PathVariable Long id,
            @Valid @RequestBody SubmitCaptchaRequest request) {
        return ResponseEntity.ok(candidateService.submitCaptcha(id, request));
    }

    // ── Refresh captcha ───────────────────────────────────────────────────────

    /**
     * POST /api/dashboard/scraping/candidates/{id}/refresh-captcha
     * Re-runs the scraper without captchaText to get a fresh CAPTCHA screenshot.
     * Updates the candidate in-place and returns the full detail response.
     * Only valid for CAPTCHA_REQUIRED or FAILED candidates.
     */
    @PostMapping("/{id}/refresh-captcha")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public ResponseEntity<ScrapeCandidateDetailResponse> refreshCaptcha(@PathVariable Long id) {
        return ResponseEntity.ok(candidateService.refreshCaptcha(id));
    }

    // ── Screenshot ────────────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/scraping/candidates/{id}/screenshot
     * Streams the saved screenshot PNG from the backend filesystem.
     * Use this URL as the img src — do not use screenshotPath directly.
     */
    @GetMapping("/{id}/screenshot")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY','REVIEWER')")
    public ResponseEntity<Resource> getScreenshot(@PathVariable Long id) {
        return candidateService.getScreenshot(id);
    }

    // ── Apply to project ──────────────────────────────────────────────────────

    /**
     * POST /api/dashboard/scraping/candidates/{id}/apply-to-project
     * Applies candidate data to a new or existing project draft.
     * The project still enters the normal DRAFT → PENDING_REVIEW review workflow.
     * Nothing is published automatically.
     */
    @PostMapping("/{id}/apply-to-project")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public ResponseEntity<ApplyToProjectResponse> applyToProject(
            @PathVariable Long id,
            @Valid @RequestBody ApplyToProjectRequest request) {
        return ResponseEntity.ok(applyService.applyToProject(id, request));
    }
}
