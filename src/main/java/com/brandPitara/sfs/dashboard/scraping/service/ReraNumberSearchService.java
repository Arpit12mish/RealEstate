package com.brandPitara.sfs.dashboard.scraping.service;

import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchRequest;
import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchResponse;
import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;

import java.util.List;

public interface ReraNumberSearchService {

    ReraNumberSearchResponse searchByReraNumber(ReraNumberSearchRequest request);

    /**
     * Clicks the CAPTCHA refresh control on an existing live browser session
     * and returns fresh screenshot bytes. Returns null if the scraper does not
     * support captcha-refresh or if the session has expired.
     */
    byte[] refreshCaptchaScreenshot(ReraSourceCode sourceCode, String sessionId, List<String> warnings);
}
