package com.brandPitara.sfs.dashboard.scraping.source;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Intermediate result returned by a ReraSourceScraper before semantic mapping.
 * Contains raw evidence (html, screenshot), extracted key-values, and scrape metadata.
 */
@Getter
@Builder
public class ReraRawSearchResult {

    private final boolean found;
    private final boolean captchaDetected;

    /** Non-null when the scraper kept a live browser session alive for CAPTCHA submission. */
    private final String captchaSessionId;

    /** When the live browser session expires (null if no active session). */
    private final OffsetDateTime captchaExpiresAt;

    private final String sourceSearchUrl;
    private final String sourceDetailUrl;
    private final String finalUrl;
    private final String title;
    private final String html;
    private final byte[] screenshotBytes;
    private final Map<String, String> extractedKeyValues;
    private final OffsetDateTime fetchedAt;

    @Builder.Default
    private final List<String> warnings = new ArrayList<>();
}
