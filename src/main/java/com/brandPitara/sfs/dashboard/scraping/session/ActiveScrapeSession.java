package com.brandPitara.sfs.dashboard.scraping.session;

import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Holds a live Playwright browser session between the initial CAPTCHA detection
 * and the admin's captcha submission. Resources are closed either by the session
 * store cleanup job (on expiry) or by the scraper after successful data extraction.
 */
@Getter
public class ActiveScrapeSession {

    private final String sessionId;
    private volatile Long candidateId;
    private final ReraSourceCode sourceCode;
    private final String reraNumber;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime expiresAt;

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;

    public ActiveScrapeSession(String sessionId,
                                Long candidateId,
                                ReraSourceCode sourceCode,
                                String reraNumber,
                                OffsetDateTime expiresAt,
                                Playwright playwright,
                                Browser browser,
                                BrowserContext context,
                                Page page) {
        this.sessionId = sessionId;
        this.candidateId = candidateId;
        this.sourceCode = sourceCode;
        this.reraNumber = reraNumber;
        this.createdAt = OffsetDateTime.now();
        this.expiresAt = expiresAt;
        this.playwright = playwright;
        this.browser = browser;
        this.context = context;
        this.page = page;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    /** Releases all Playwright resources. Safe to call multiple times. */
    public void close() {
        try { context.close(); }  catch (Exception ignored) {}
        try { browser.close(); }  catch (Exception ignored) {}
        try { playwright.close(); } catch (Exception ignored) {}
    }
}
