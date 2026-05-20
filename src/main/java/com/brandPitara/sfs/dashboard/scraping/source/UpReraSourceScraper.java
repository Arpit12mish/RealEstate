package com.brandPitara.sfs.dashboard.scraping.source;

import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.brandPitara.sfs.dashboard.scraping.parser.ReraDetailKeyValueParser;
import com.brandPitara.sfs.dashboard.scraping.session.ActiveScrapeSession;
import com.brandPitara.sfs.dashboard.scraping.session.ScrapeSessionStore;
import com.brandPitara.sfs.dashboard.scraping.util.ScrapeCaptchaDetector;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpReraSourceScraper implements CaptchaCapableScraper {

    private static final String SEARCH_URL = "https://uprera.azurewebsites.net/View_projects.aspx";
    private static final String HOME_URL = "https://uprera.azurewebsites.net/";

    private static final String REGISTRATION_INPUT = "#ctl00_ContentPlaceHolder1_txt_regid1";
    private static final String CAPTCHA_INPUT = "#ctl00_ContentPlaceHolder1_txtcap";
    private static final String CAPTCHA_REFRESH = "#ctl00_ContentPlaceHolder1_btn_refresh1";
    private static final String SEARCH_BUTTON = "#ctl00_ContentPlaceHolder1_btnSearch";

    private static final String INVALID_CAPTCHA_TEXT = "invalid captcha";
    private static final String COMPLETED_PROJECT_TEXT = "completed project";
    private static final long SESSION_TTL_MINUTES = 5;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final String JS_FIND_DETAIL_URL_OR_CLICK_ROW_VIEW = """
            (reraNumber) => {
                const normalize = (v) => (v || "")
                    .toUpperCase()
                    .replace(/\\s+/g, "")
                    .replace(/[^A-Z0-9/]/g, "");

                const target = normalize(reraNumber);
                const rows = Array.from(document.querySelectorAll("table tbody tr, table tr"));

                for (const row of rows) {
                    const rowText = normalize(row.innerText || row.textContent || "");

                    const cells = Array.from(row.querySelectorAll("td, th"))
                        .map(c => normalize(c.innerText || c.textContent || ""));

                    const matched =
                        rowText.includes(target) ||
                        target.includes(rowText) ||
                        cells.some(c => c.length >= 4 && (c.includes(target) || target.includes(c)));

                    if (!matched) {
                        continue;
                    }

                    const links = Array.from(row.querySelectorAll("a[href]"));
                    const detailLink = links.find(a =>
                        /view|detail|project|complete/i.test(a.innerText || a.textContent || a.href || "")
                    ) || links[0];

                    if (detailLink) {
                        return {
                            type: "HREF",
                            href: detailLink.href,
                            rowText: row.innerText || row.textContent || ""
                        };
                    }

                    const clickable = Array.from(row.querySelectorAll(
                        "button, input[type='submit'], input[type='button'], a"
                    )).find(el =>
                        /view|detail/i.test(
                            (el.innerText || "") + " " + (el.value || "") + " " + (el.title || "")
                        )
                    ) || row.querySelector("button, input[type='submit'], input[type='button'], a");

                    if (clickable) {
                        clickable.scrollIntoView({ block: "center", inline: "center" });
                        clickable.click();

                        return {
                            type: "CLICKED_ROW_ACTION",
                            rowText: row.innerText || row.textContent || ""
                        };
                    }

                    row.scrollIntoView({ block: "center", inline: "center" });
                    row.click();

                    return {
                        type: "CLICKED_ROW",
                        rowText: row.innerText || row.textContent || ""
                    };
                }

                return null;
            }
            """;

    private final ReraDetailKeyValueParser keyValueParser;
    private final ScrapeCaptchaDetector captchaDetector;
    private final ScrapeSessionStore sessionStore;

    @Override
    public boolean supports(ReraSourceCode sourceCode) {
        return ReraSourceCode.UP_RERA == sourceCode;
    }

    @Override
    public ReraRawSearchResult searchByReraNumber(String reraNumber, String captchaText) {
        return scrapeWithPlaywright(reraNumber, captchaText);
    }

    @Override
    public ReraRawSearchResult continueWithCaptcha(String sessionId, String captchaText) {
        List<String> warnings = new ArrayList<>();
        OffsetDateTime fetchedAt = OffsetDateTime.now();

        ActiveScrapeSession session = sessionStore.get(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "CAPTCHA session not found or expired: " + sessionId +
                                ". Please start a new scrape."));

        Page page = session.getPage();
        String reraNumber = session.getReraNumber();

        log.info("UP RERA: continuing with captcha. sessionId={} reraNumber={}", sessionId, reraNumber);

        try {
            fillCaptcha(page, captchaText.trim(), warnings);
            probePage(page, reraNumber, "POST_CAPTCHA_FILL", warnings);

            submitSearch(page, warnings);
            waitAfterSearchSubmit(page, warnings);

            probePage(page, reraNumber, "POST_CAPTCHA_SEARCH", warnings);

            if (isInvalidCaptchaShown(page)) {
                warnings.add("UP_RERA: Invalid captcha text. Admin should retry with the current captcha.");
                log.info("UP RERA: invalid captcha submitted. sessionId={}", sessionId);

                return captchaRequiredResultWithSession(
                        page,
                        SEARCH_URL,
                        sessionId,
                        session.getExpiresAt(),
                        fetchedAt,
                        warnings
                );
            }

            boolean resultVisible = isSearchResultVisible(page, reraNumber, warnings);
            boolean completedProjectAlertVisible = isCompletedProjectAlertVisible(page);

            if (!resultVisible && !completedProjectAlertVisible) {
                warnings.add("UP_RERA: Captcha accepted status unclear. No result row or completed-project alert detected.");
                return captchaRequiredResultWithSession(
                        page,
                        SEARCH_URL,
                        sessionId,
                        session.getExpiresAt(),
                        fetchedAt,
                        warnings
                );
            }

            warnings.add("UP_RERA: Captcha accepted. Search result is visible.");

            ReraRawSearchResult result = extractAfterSuccessfulSearch(
                    page,
                    reraNumber,
                    fetchedAt,
                    warnings
            );

            sessionStore.remove(sessionId);
            return result;

        } catch (Exception e) {
            warnings.add("UP_RERA: Captcha continuation failed but session is kept alive: " + e.getMessage());
            warnings.add("UP_RERA: bodySampleAfterContinuationFailure=" + bodyTextSample(page));

            log.warn("UP RERA: captcha continuation failed. Keeping session alive. sessionId={} reason={}",
                    sessionId, e.getMessage());

            return captchaRequiredResultWithSession(
                    page,
                    SEARCH_URL,
                    sessionId,
                    session.getExpiresAt(),
                    fetchedAt,
                    warnings
            );
        }
    }

    @Override
    public byte[] refreshCaptchaScreenshot(String sessionId, List<String> warnings) {
        ActiveScrapeSession session = sessionStore.get(sessionId).orElse(null);
        if (session == null) {
            warnings.add("UP_RERA: Cannot refresh captcha — session not found or expired: " + sessionId);
            return null;
        }

        Page page = session.getPage();

        try {
            Object result = page.evaluate("""
                    (selector) => {
                        const btn = document.querySelector(selector);
                        if (!btn) {
                            return {
                                ok: false,
                                reason: "refresh_button_not_found",
                                selector
                            };
                        }

                        try {
                            btn.scrollIntoView({ block: "center", inline: "center" });
                        } catch (e) {
                            // ignore
                        }

                        try {
                            btn.click();
                            return {
                                ok: true,
                                strategy: "DOM_CLICK",
                                id: btn.id,
                                name: btn.name,
                                type: btn.type
                            };
                        } catch (e) {
                            return {
                                ok: false,
                                reason: e.message,
                                id: btn.id,
                                name: btn.name,
                                type: btn.type
                            };
                        }
                    }
                    """, CAPTCHA_REFRESH);

            warnings.add("UP_RERA: Refresh captcha click result: " + result);
            page.waitForTimeout(1_500);

            return captureScreenshot(page, warnings);

        } catch (Exception e) {
            warnings.add("UP_RERA: Failed to refresh captcha image: " + e.getMessage());
            return captureScreenshot(page, warnings);
        }
    }

    private ReraRawSearchResult scrapeWithPlaywright(String reraNumber, String captchaText) {
        List<String> warnings = new ArrayList<>();
        OffsetDateTime fetchedAt = OffsetDateTime.now();

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        boolean keepAlive = false;

        try {
            playwright = Playwright.create();
            browser = launchBrowser(playwright);
            context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1440, 1200)
                            .setUserAgent(USER_AGENT)
            );
            page = context.newPage();

            log.info("UP RERA: visiting home page to initialize session. reraNumber={}", reraNumber);
            navigateSoft(page, HOME_URL, warnings);

            log.info("UP RERA: opening search page. reraNumber={}", reraNumber);
            navigateTo(page, SEARCH_URL);

            boolean formReady = waitForUpReraForm(page, warnings);
            probePage(page, reraNumber, "POST_LOAD", warnings);

            if (!formReady) {
                warnings.add("UP_RERA: Current WebForms search form did not appear.");
                return noResultFound(page, SEARCH_URL, fetchedAt, warnings);
            }

            fillRegistrationNumber(page, reraNumber, warnings);
            probePage(page, reraNumber, "POST_REGISTRATION_FILL", warnings);

            if (isCaptchaFieldVisible(page)) {
                if (!StringUtils.hasText(captchaText)) {
                    warnings.add("UP_RERA: CAPTCHA field visible. Re-submit with captchaText to complete search.");
                    warnings.add("UP_RERA: Registration number filled. Screenshot captured for captcha reading.");

                    String sessionId = UUID.randomUUID().toString();
                    OffsetDateTime expires = OffsetDateTime.now().plusMinutes(SESSION_TTL_MINUTES);

                    ActiveScrapeSession session = new ActiveScrapeSession(
                            sessionId,
                            null,
                            ReraSourceCode.UP_RERA,
                            reraNumber,
                            expires,
                            playwright,
                            browser,
                            context,
                            page
                    );

                    sessionStore.put(session);
                    keepAlive = true;

                    return captchaRequiredResultWithSession(
                            page,
                            SEARCH_URL,
                            sessionId,
                            expires,
                            fetchedAt,
                            warnings
                    );
                }

                fillCaptcha(page, captchaText.trim(), warnings);
            }

            submitSearch(page, warnings);
            waitAfterSearchSubmit(page, warnings);
            probePage(page, reraNumber, "POST_SEARCH", warnings);

            if (isInvalidCaptchaShown(page)) {
                warnings.add("UP_RERA: Invalid captcha after direct scrape attempt.");
                return captchaRequiredResult(page, SEARCH_URL, fetchedAt, warnings);
            }

            boolean resultVisible = isSearchResultVisible(page, reraNumber, warnings);
            boolean completedProjectAlertVisible = isCompletedProjectAlertVisible(page);

            if (!resultVisible && !completedProjectAlertVisible) {
                warnings.add("UP_RERA: No result rows appeared after search.");
                return noResultFound(page, SEARCH_URL, fetchedAt, warnings);
            }

            return extractAfterSuccessfulSearch(page, reraNumber, fetchedAt, warnings);

        } catch (IllegalStateException e) {
            throw e;
        } catch (PlaywrightException e) {
            throw new IllegalStateException(
                    "Playwright error during UP RERA scrape: " + e.getMessage(), e
            );
        } finally {
            if (!keepAlive) {
                closeQuietly(context, browser, playwright);
            }
        }
    }

    private ReraRawSearchResult extractAfterSuccessfulSearch(
            Page page,
            String reraNumber,
            OffsetDateTime fetchedAt,
            List<String> warnings
    ) {
        String listingHtml = page.content();
        byte[] listingScreenshot = captureScreenshot(page, warnings);

        Map<String, String> listingValues = extractListingRowValues(page, reraNumber, warnings);

        if (isCompletedProjectAlertVisible(page)) {
            warnings.add("UP_RERA: Completed project alert detected. Trying to open completion details.");

            boolean opened = clickCompletedProjectViewDetails(page, warnings);

            if (opened) {
                page.waitForTimeout(2_000);
                probePage(page, reraNumber, "POST_COMPLETION_DETAILS_CLICK", warnings);
            } else {
                warnings.add("UP_RERA: Could not click completed-project View Details. Falling back to listing row values.");
            }
        } else {
            Object detailAction = evalObject(page, JS_FIND_DETAIL_URL_OR_CLICK_ROW_VIEW, reraNumber, warnings);
            warnings.add("UP_RERA: Detail action result: " + detailAction);

            String detailUrlFromJs = extractHrefFromJsResult(detailAction);

            if (StringUtils.hasText(detailUrlFromJs)) {
                navigateTo(page, detailUrlFromJs);
            } else if (detailAction != null) {
                page.waitForTimeout(2_000);
                probePage(page, reraNumber, "POST_ROW_DETAIL_CLICK", warnings);
            }
        }

        String detailHtml = page.content();
        String title = page.title();
        String finalUrl = page.url();
        byte[] screenshot = captureScreenshot(page, warnings);

        Map<String, String> keyValues = keyValueParser.parse(detailHtml, finalUrl);

        if (keyValues.isEmpty()) {
            warnings.add("UP_RERA: Detail parser extracted 0 fields. Using listing row fallback values.");
            keyValues = listingValues;
        } else {
            keyValues.putIfAbsent("rera_number", reraNumber);
            listingValues.forEach(keyValues::putIfAbsent);
        }

        boolean found = !keyValues.isEmpty() || !listingValues.isEmpty();

        if (!found) {
            warnings.add("UP_RERA: Search result was visible, but no structured values were extracted.");
            return ReraRawSearchResult.builder()
                    .found(false)
                    .captchaDetected(false)
                    .sourceSearchUrl(SEARCH_URL)
                    .sourceDetailUrl(null)
                    .finalUrl(finalUrl)
                    .title(title)
                    .html(detailHtml != null ? detailHtml : listingHtml)
                    .screenshotBytes(screenshot != null ? screenshot : listingScreenshot)
                    .extractedKeyValues(Map.of())
                    .fetchedAt(fetchedAt)
                    .warnings(warnings)
                    .build();
        }

        warnings.add("UP_RERA: Extracted " + keyValues.size() + " field(s). Captcha was accepted.");

        return ReraRawSearchResult.builder()
                .found(true)
                .captchaDetected(false)
                .sourceSearchUrl(SEARCH_URL)
                .sourceDetailUrl(finalUrl)
                .finalUrl(finalUrl)
                .title(title)
                .html(detailHtml != null ? detailHtml : listingHtml)
                .screenshotBytes(screenshot != null ? screenshot : listingScreenshot)
                .extractedKeyValues(keyValues)
                .fetchedAt(fetchedAt)
                .warnings(warnings)
                .build();
    }

    private boolean clickCompletedProjectViewDetails(Page page, List<String> warnings) {
        try {
            Object result = page.evaluate("""
                    () => {
                        const textOf = (el) => (
                            (el.innerText || "") + " " +
                            (el.textContent || "") + " " +
                            (el.value || "") + " " +
                            (el.title || "")
                        ).trim();

                        const candidates = Array.from(document.querySelectorAll(
                            "button, input[type='button'], input[type='submit'], a"
                        ));

                        const target = candidates.find(el =>
                            /view\\s*details/i.test(textOf(el))
                        ) || candidates.find(el =>
                            /details/i.test(textOf(el))
                        );

                        if (!target) {
                            return {
                                ok: false,
                                reason: "view_details_button_not_found",
                                buttons: candidates.slice(0, 20).map(textOf)
                            };
                        }

                        try {
                            target.scrollIntoView({ block: "center", inline: "center" });
                        } catch (e) {
                            // ignore
                        }

                        try {
                            target.click();
                            return {
                                ok: true,
                                strategy: "DOM_CLICK",
                                text: textOf(target),
                                id: target.id,
                                name: target.name,
                                href: target.href || null
                            };
                        } catch (e) {
                            return {
                                ok: false,
                                reason: e.message,
                                text: textOf(target),
                                id: target.id,
                                name: target.name
                            };
                        }
                    }
                    """);

            warnings.add("UP_RERA: Completed project View Details click result: " + result);
            return result != null && result.toString().contains("ok=true");

        } catch (Exception e) {
            warnings.add("UP_RERA: Failed to click completed-project View Details: " + e.getMessage());
            return false;
        }
    }

    private Map<String, String> extractListingRowValues(Page page, String reraNumber, List<String> warnings) {
        try {
            Object result = page.evaluate("""
                    (reraNumber) => {
                        const normalize = (v) => (v || "")
                            .toUpperCase()
                            .replace(/\\s+/g, "")
                            .replace(/[^A-Z0-9/]/g, "");

                        const target = normalize(reraNumber);
                        const rows = Array.from(document.querySelectorAll("table tbody tr, table tr"));

                        for (const row of rows) {
                            const cells = Array.from(row.querySelectorAll("td, th"))
                                .map(c => (c.innerText || c.textContent || "").trim());

                            if (cells.length < 3) {
                                continue;
                            }

                            const rowText = normalize(cells.join(" "));
                            if (!rowText.includes(target)) {
                                continue;
                            }

                            return {
                                serial_number: cells[0] || "",
                                rera_number: cells[1] || reraNumber,
                                project_name: cells[2] || "",
                                promoter_name: cells[3] || "",
                                district: cells[4] || "",
                                project_type: cells[5] || "",
                                approval_certificate: cells[6] || ""
                            };
                        }

                        return {};
                    }
                    """, reraNumber);

            Map<String, String> map = objectToStringMap(result);
            warnings.add("UP_RERA: Listing row fallback values: " + map);
            return map;

        } catch (Exception e) {
            warnings.add("UP_RERA: Failed to extract listing row values: " + e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> objectToStringMap(Object result) {
        if (!(result instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        Map<String, String> output = new LinkedHashMap<>();

        rawMap.forEach((key, value) -> {
            if (key != null && value != null) {
                String k = String.valueOf(key).trim();
                String v = String.valueOf(value).trim();

                if (!k.isBlank() && !v.isBlank()) {
                    output.put(k, v);
                }
            }
        });

        return output;
    }

    @SuppressWarnings("unchecked")
    private String extractHrefFromJsResult(Object detailAction) {
        if (detailAction instanceof Map<?, ?> map) {
            Object href = map.get("href");
            return href == null ? null : String.valueOf(href);
        }

        return null;
    }

    private boolean isSearchResultVisible(Page page, String reraNumber, List<String> warnings) {
        try {
            Object result = page.evaluate("""
                    (reraNumber) => {
                        const normalize = (v) => (v || "")
                            .toUpperCase()
                            .replace(/\\s+/g, "")
                            .replace(/[^A-Z0-9/]/g, "");

                        const target = normalize(reraNumber);

                        const rows = Array.from(document.querySelectorAll("table tbody tr, table tr"))
                            .filter(row => {
                                const text = (row.innerText || row.textContent || "").trim();
                                return text.length > 0;
                            });

                        const matchingRows = rows.filter(row => {
                            const text = normalize(row.innerText || row.textContent || "");
                            return text.includes(target);
                        });

                        return {
                            rowCount: rows.length,
                            matchingRowCount: matchingRows.length,
                            firstMatchingRow: matchingRows.length > 0
                                ? (matchingRows[0].innerText || matchingRows[0].textContent || "").trim()
                                : null
                        };
                    }
                    """, reraNumber);

            warnings.add("UP_RERA: Result visibility check: " + result);
            return result != null && result.toString().contains("matchingRowCount=1");

        } catch (Exception e) {
            warnings.add("UP_RERA: Result visibility check failed: " + e.getMessage());
            return false;
        }
    }

    private boolean isCompletedProjectAlertVisible(Page page) {
        try {
            String body = page.locator("body").innerText();
            return body != null && body.toLowerCase().contains(COMPLETED_PROJECT_TEXT);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInvalidCaptchaShown(Page page) {
        try {
            String body = page.locator("body").innerText();
            return body != null && body.toLowerCase().contains(INVALID_CAPTCHA_TEXT);
        } catch (Exception e) {
            return false;
        }
    }

    private void submitSearch(Page page, List<String> warnings) {
        try {
            Object result = page.evaluate("""
                    (selector) => {
                        const btn = document.querySelector(selector);

                        if (!btn) {
                            return {
                                ok: false,
                                strategy: "NONE",
                                reason: "search_button_not_found",
                                selector
                            };
                        }

                        try {
                            btn.scrollIntoView({ block: "center", inline: "center" });
                        } catch (e) {
                            // ignore
                        }

                        try {
                            btn.disabled = false;
                            btn.removeAttribute("disabled");
                        } catch (e) {
                            // ignore
                        }

                        try {
                            btn.focus();
                        } catch (e) {
                            // ignore
                        }

                        try {
                            btn.click();
                            return {
                                ok: true,
                                strategy: "DOM_CLICK",
                                id: btn.id,
                                name: btn.name,
                                type: btn.type,
                                value: btn.value
                            };
                        } catch (clickError) {
                            const form = btn.closest("form");

                            if (!form) {
                                return {
                                    ok: false,
                                    strategy: "DOM_CLICK",
                                    reason: clickError.message,
                                    formFound: false
                                };
                            }

                            try {
                                if (typeof form.requestSubmit === "function") {
                                    form.requestSubmit(btn);
                                    return {
                                        ok: true,
                                        strategy: "REQUEST_SUBMIT",
                                        formId: form.id,
                                        buttonId: btn.id
                                    };
                                }

                                form.submit();
                                return {
                                    ok: true,
                                    strategy: "FORM_SUBMIT",
                                    formId: form.id,
                                    buttonId: btn.id
                                };
                            } catch (submitError) {
                                return {
                                    ok: false,
                                    strategy: "FORM_SUBMIT",
                                    clickReason: clickError.message,
                                    submitReason: submitError.message,
                                    formId: form.id,
                                    buttonId: btn.id
                                };
                            }
                        }
                    }
                    """, SEARCH_BUTTON);

            warnings.add("UP_RERA: Search submit result: " + result);

            if (result == null || !result.toString().contains("ok=true")) {
                throw new IllegalStateException("UP RERA search submit did not confirm success: " + result);
            }

        } catch (Exception e) {
            warnings.add("UP_RERA: Failed while submitting search form: " + e.getMessage());
            warnings.add("UP_RERA: bodySampleBeforeSubmitFailure=" + bodyTextSample(page));
            throw new IllegalStateException("Unable to submit UP RERA search form", e);
        }
    }

    private void waitAfterSearchSubmit(Page page, List<String> warnings) {
        try {
            page.waitForFunction(
                    """
                    () => {
                        const bodyText = (document.body && document.body.innerText || "").toLowerCase();

                        const invalidCaptcha =
                            bodyText.includes("invalid captcha") ||
                            bodyText.includes("invalid captcha!!!");

                        const completedProject =
                            bodyText.includes("completed project") ||
                            bodyText.includes("click here to view completion details");

                        const noRecord =
                            bodyText.includes("no record") ||
                            bodyText.includes("no data") ||
                            bodyText.includes("not found");

                        const visibleRows = Array.from(
                            document.querySelectorAll("table tbody tr, table tr")
                        ).filter(row => {
                            const text = (row.innerText || row.textContent || "").trim();
                            return text.length > 0;
                        });

                        return invalidCaptcha || completedProject || noRecord || visibleRows.length > 1;
                    }
                    """,
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(20_000)
            );

            warnings.add("UP_RERA: Search submit produced a visible result state.");

        } catch (TimeoutError e) {
            warnings.add("UP_RERA: Search submit wait timed out after 20s. Continuing with page probe.");
        } catch (Exception e) {
            warnings.add("UP_RERA: Search submit wait failed: " + e.getMessage());
        }

        page.waitForTimeout(1_500);
    }

    private void closeQuietly(BrowserContext context, Browser browser, Playwright playwright) {
        try {
            if (context != null) context.close();
        } catch (Exception ignored) {
        }
        try {
            if (browser != null) browser.close();
        } catch (Exception ignored) {
        }
        try {
            if (playwright != null) playwright.close();
        } catch (Exception ignored) {
        }
    }

    private boolean waitForUpReraForm(Page page, List<String> warnings) {
        try {
            page.waitForSelector(
                    REGISTRATION_INPUT,
                    new Page.WaitForSelectorOptions().setTimeout(20_000)
            );
            page.waitForSelector(
                    SEARCH_BUTTON,
                    new Page.WaitForSelectorOptions().setTimeout(20_000)
            );
            return true;
        } catch (TimeoutError e) {
            warnings.add("UP_RERA: WebForms search form selectors not found within 20s.");
            warnings.add("UP_RERA: expected registration selector=" + REGISTRATION_INPUT);
            warnings.add("UP_RERA: expected search selector=" + SEARCH_BUTTON);
            return false;
        }
    }

    private void fillRegistrationNumber(Page page, String reraNumber, List<String> warnings) {
        try {
            page.locator(REGISTRATION_INPUT).fill("");
            page.locator(REGISTRATION_INPUT).fill(reraNumber);
            warnings.add("UP_RERA: Filled registration number using selector " + REGISTRATION_INPUT);
        } catch (Exception e) {
            warnings.add("UP_RERA: Failed to fill registration number: " + e.getMessage());
            throw new IllegalStateException("Unable to fill UP RERA registration number", e);
        }
    }

    private boolean isCaptchaFieldVisible(Page page) {
        try {
            return page.locator(CAPTCHA_INPUT).isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    private void fillCaptcha(Page page, String captchaText, List<String> warnings) {
        try {
            page.locator(CAPTCHA_INPUT).scrollIntoViewIfNeeded();
            page.locator(CAPTCHA_INPUT).fill("");

            page.evaluate("""
                    (args) => {
                        const input = document.querySelector(args.selector);
                        if (!input) return false;

                        input.focus();

                        try {
                            const nativeSetter = Object.getOwnPropertyDescriptor(
                                window.HTMLInputElement.prototype,
                                "value"
                            ).set;
                            nativeSetter.call(input, args.value);
                        } catch (e) {
                            input.value = args.value;
                        }

                        ["input", "change", "keyup", "blur"].forEach(type => {
                            input.dispatchEvent(new Event(type, { bubbles: true }));
                        });

                        return true;
                    }
                    """, Map.of("selector", CAPTCHA_INPUT, "value", captchaText.trim()));

            warnings.add("UP_RERA: Filled captcha using selector " + CAPTCHA_INPUT);
        } catch (Exception e) {
            warnings.add("UP_RERA: Failed to fill captcha: " + e.getMessage());
            throw new IllegalStateException("Unable to fill UP RERA captcha input", e);
        }
    }

    private void probePage(Page page, String reraNumber, String label, List<String> warnings) {
        try {
            Object probe = page.evaluate("""
                    (args) => {
                        const { reraNumber, label } = args;

                        const fields = Array.from(document.querySelectorAll("input, select, button, img"))
                            .map((e, index) => ({
                                index,
                                tag: e.tagName,
                                id: e.id,
                                name: e.name,
                                type: e.type,
                                value: e.type === "password" ? null : e.value,
                                placeholder: e.placeholder,
                                src: e.src,
                                text: e.innerText || e.value || e.placeholder || ""
                            }))
                            .filter(x =>
                                x.id ||
                                x.name ||
                                x.text ||
                                x.src ||
                                x.tag === "SELECT"
                            )
                            .slice(0, 40);

                        const rows = Array.from(document.querySelectorAll("table tbody tr, table tr"))
                            .slice(0, 8)
                            .map(row => (row.innerText || row.textContent || "").trim().substring(0, 700));

                        return {
                            label,
                            url: location.href,
                            title: document.title,
                            bodyHasRera: document.body.innerText.toUpperCase().includes(reraNumber.toUpperCase()),
                            bodySample: document.body.innerText.substring(0, 1000),
                            registrationInputExists: !!document.querySelector("#ctl00_ContentPlaceHolder1_txt_regid1"),
                            captchaInputExists: !!document.querySelector("#ctl00_ContentPlaceHolder1_txtcap"),
                            searchButtonExists: !!document.querySelector("#ctl00_ContentPlaceHolder1_btnSearch"),
                            fields,
                            rowCount: rows.length,
                            rows
                        };
                    }
                    """, Map.of("reraNumber", reraNumber, "label", label));

            log.info("UP RERA probe [{}]: {}", label, probe);
            warnings.add("UP_RERA probe [" + label + "]: " + probe);
        } catch (Exception e) {
            warnings.add("UP_RERA probe [" + label + "] failed: " + e.getMessage());
        }
    }

    private void navigateSoft(Page page, String url, List<String> warnings) {
        try {
            page.navigate(
                    url,
                    new Page.NavigateOptions()
                            .setTimeout(30_000)
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            try {
                page.waitForLoadState(
                        LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(10_000)
                );
            } catch (TimeoutError ignored) {
                warnings.add("UP_RERA: soft navigation network idle timeout ignored for " + url);
            }
        } catch (Exception e) {
            warnings.add("UP_RERA: soft navigate to " + url + " failed: " + e.getMessage());
        }
    }

    private void navigateTo(Page page, String url) {
        try {
            page.navigate(
                    url,
                    new Page.NavigateOptions()
                            .setTimeout(60_000)
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            );

            try {
                page.waitForLoadState(
                        LoadState.NETWORKIDLE,
                        new Page.WaitForLoadStateOptions().setTimeout(20_000)
                );
            } catch (TimeoutError ignored) {
                // ASP.NET pages can keep background requests alive.
            }
        } catch (TimeoutError e) {
            throw new IllegalStateException("Timeout navigating to: " + url, e);
        }
    }

    private Object evalObject(Page page, String jsExpression, String arg, List<String> warnings) {
        try {
            return arg != null
                    ? page.evaluate(jsExpression, arg)
                    : page.evaluate(jsExpression);
        } catch (Exception e) {
            warnings.add("UP_RERA JS eval failed: " + e.getMessage());
            log.warn("UP RERA page.evaluate failed: {}", e.getMessage());
            return null;
        }
    }

    private Browser launchBrowser(Playwright playwright) {
        try {
            return playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
        } catch (PlaywrightException e) {
            throw new IllegalStateException(
                    "Playwright browser could not be launched. " +
                            "Run: mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI " +
                            "-Dexec.args=\"install chromium\"",
                    e
            );
        }
    }

    private byte[] captureScreenshot(Page page, List<String> warnings) {
        try {
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (Exception e) {
            warnings.add("UP_RERA screenshot failed: " + e.getMessage());
            return null;
        }
    }

    private String bodyTextSample(Page page) {
        try {
            String bodyText = page.locator("body").innerText();
            if (bodyText == null || bodyText.isBlank()) {
                return null;
            }
            return bodyText.length() > 1200 ? bodyText.substring(0, 1200) : bodyText;
        } catch (Exception e) {
            return "Unable to read body text: " + e.getMessage();
        }
    }

    private ReraRawSearchResult captchaRequiredResultWithSession(
            Page page,
            String searchUrl,
            String sessionId,
            OffsetDateTime expiresAt,
            OffsetDateTime fetchedAt,
            List<String> warnings
    ) {
        return ReraRawSearchResult.builder()
                .found(false)
                .captchaDetected(true)
                .captchaSessionId(sessionId)
                .captchaExpiresAt(expiresAt)
                .sourceSearchUrl(searchUrl)
                .sourceDetailUrl(null)
                .finalUrl(page.url())
                .title(page.title())
                .html(page.content())
                .screenshotBytes(captureScreenshot(page, warnings))
                .extractedKeyValues(Map.of())
                .fetchedAt(fetchedAt)
                .warnings(warnings)
                .build();
    }

    private ReraRawSearchResult captchaRequiredResult(
            Page page,
            String searchUrl,
            OffsetDateTime fetchedAt,
            List<String> warnings
    ) {
        return ReraRawSearchResult.builder()
                .found(false)
                .captchaDetected(true)
                .sourceSearchUrl(searchUrl)
                .sourceDetailUrl(null)
                .finalUrl(page.url())
                .title(page.title())
                .html(page.content())
                .screenshotBytes(captureScreenshot(page, warnings))
                .extractedKeyValues(Map.of())
                .fetchedAt(fetchedAt)
                .warnings(warnings)
                .build();
    }

    private ReraRawSearchResult noResultFound(
            Page page,
            String searchUrl,
            OffsetDateTime fetchedAt,
            List<String> warnings
    ) {
        return ReraRawSearchResult.builder()
                .found(false)
                .captchaDetected(false)
                .sourceSearchUrl(searchUrl)
                .sourceDetailUrl(null)
                .finalUrl(page.url())
                .title(page.title())
                .html(page.content())
                .screenshotBytes(captureScreenshot(page, warnings))
                .extractedKeyValues(Map.of())
                .fetchedAt(fetchedAt)
                .warnings(warnings)
                .build();
    }
}