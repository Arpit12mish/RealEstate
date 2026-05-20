package com.brandPitara.sfs.dashboard.scraping.source;

import java.util.List;

/**
 * Extension of {@link ReraSourceScraper} for portals that present CAPTCHA challenges.
 *
 * The workflow is:
 *   1. {@code searchByReraNumber(reraNumber, null)} — opens portal, fills form, detects CAPTCHA,
 *      keeps browser alive in {@link com.brandPitara.sfs.dashboard.scraping.session.ScrapeSessionStore},
 *      returns result with {@code captchaSessionId}.
 *   2. {@code continueWithCaptcha(sessionId, captchaText)} — resumes the live browser session,
 *      fills the CAPTCHA field, clicks search, and extracts data.
 *   3. {@code refreshCaptchaScreenshot(sessionId, warnings)} — clicks the portal's CAPTCHA
 *      refresh button and returns fresh screenshot bytes for the dashboard.
 */
public interface CaptchaCapableScraper extends ReraSourceScraper {

    /**
     * Continues scraping using an existing live browser session.
     * Returns a new {@code captchaDetected=true} result if the captcha text was wrong.
     */
    ReraRawSearchResult continueWithCaptcha(String sessionId, String captchaText);

    /**
     * Clicks the portal's captcha-refresh control and returns a new screenshot.
     * Returns null if the refresh fails; callers should log warnings accordingly.
     */
    byte[] refreshCaptchaScreenshot(String sessionId, List<String> warnings);
}
