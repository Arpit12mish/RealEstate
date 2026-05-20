package com.brandPitara.sfs.dashboard.scraping.candidate.enums;

public enum ScrapeCandidateStatus {

    /** Scrape started but not yet complete (reserved for future async job design). */
    PENDING,

    /** Portal opened and form filled, but manual CAPTCHA entry is required to continue. */
    CAPTCHA_REQUIRED,

    /** Useful data extracted (confidence ≥ 70%). Ready for data-entry review. */
    READY_FOR_REVIEW,

    /** Some data extracted but important fields are missing (confidence < 70%). */
    PARTIAL,

    // ── Legacy / workflow states ──────────────────────────────────────────────
    /** Legacy: scrape completed. Use READY_FOR_REVIEW or PARTIAL for new candidates. */
    SCRAPED,
    NEEDS_REVIEW,
    READY_TO_APPLY,
    APPLIED,
    REJECTED,
    FAILED;

    public boolean isTerminal() {
        return this == APPLIED || this == REJECTED;
    }

    public boolean isApplyable() {
        return this == READY_TO_APPLY
                || this == SCRAPED
                || this == NEEDS_REVIEW
                || this == READY_FOR_REVIEW
                || this == PARTIAL;
    }
}
