package com.brandPitara.sfs.dashboard.scraping.source;

import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;

/**
 * Strategy interface for source-specific RERA portal scrapers.
 *
 * Each implementation handles one RERA authority's portal (navigation, form interaction,
 * result extraction). The scraper returns ReraRawSearchResult which is then semantically
 * mapped by ReraProjectCandidateMapper.
 *
 * Phase S1B implementation: HaryanaReraSourceScraper.
 * Future: MahaReraSourceScraper, UpReraSourceScraper, etc.
 */
public interface ReraSourceScraper {

    boolean supports(ReraSourceCode sourceCode);

    /**
     * Scrapes the RERA portal for the given registration number.
     *
     * @param reraNumber  registration number to search
     * @param captchaText CAPTCHA answer typed by the admin, or null on the first attempt
     */
    ReraRawSearchResult searchByReraNumber(String reraNumber, String captchaText);

    /** Convenience overload — delegates with no captcha (first-attempt shorthand). */
    default ReraRawSearchResult searchByReraNumber(String reraNumber) {
        return searchByReraNumber(reraNumber, null);
    }
}
