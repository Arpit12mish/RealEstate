package com.brandPitara.sfs.dashboard.scraping.source;

import com.brandPitara.sfs.dashboard.scraping.enums.ReraSourceCode;
import com.brandPitara.sfs.dashboard.scraping.parser.ReraDetailKeyValueParser;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Haryana RERA portal scraper.
 * Portal: https://haryanarera.gov.in
 *
 * HOW THE PORTAL WORKS:
 *   - No direct RERA number text input — search is by district only
 *   - No CAPTCHA on the public search page
 *   - POST district value → portal returns all projects for that district (server-rendered HTML)
 *   - Each result row contains the "Project Registration Number" column (column index 3)
 *   - We match the RERA number against that column, then navigate to the project detail page
 *   - We also navigate to the Form A-H page (project_preview_open) for richer builder data
 *
 * DISTRICT PREFIX MAPPING:
 *   GGM → Gurugram (62), PKL → Panchkula (70)
 *   Unknown prefixes fall back to "999" (all districts — slower but universal).
 *
 * STATUS: ACTIVE — no manual selector configuration needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HaryanaReraSourceScraper implements ReraSourceScraper {

    // -------------------------------------------------------------------------
    // Portal constants (verified against live site)
    // -------------------------------------------------------------------------

    private static final String SEARCH_URL           = "https://haryanarera.gov.in/assistancecontrol/project_search_public/2";
    private static final String RESULTS_ROW_SELECTOR = "#compliant_hearing tbody tr";

    // RERA number first segment → district <select> value
    // District values from the portal's own dropdown (verified)
    private static final Map<String, String> RERA_PREFIX_TO_DISTRICT = Map.of(
            "GGM", "62",   // HRERA Gurugram
            "PKL", "70"    // HRERA Panchkula
    );

    // JavaScript run inside the browser to find the matching result row
    private static final String JS_FIND_DETAIL_URL = """
            (reraNumber) => {
                const normalize = (value) => (value || "")
                    .toUpperCase()
                    .replace(/\\s+/g, "")
                    .replace(/[^A-Z0-9/]/g, "");

                const target = normalize(reraNumber);
                const rows = document.querySelectorAll('#compliant_hearing tbody tr');

                for (const row of rows) {
                    const cells = row.querySelectorAll('th, td');
                    if (cells.length >= 4) {
                        const regNum = normalize(cells[3].textContent);

                        if (regNum.includes(target) || target.includes(regNum)) {
                            const link = cells[1].querySelector('a');
                            return link ? link.href : null;
                        }
                    }
                }

                return null;
            }
            """;

    private static final String JS_FIND_FORM_AH_URL = """
            (reraNumber) => {
                const normalize = (value) => (value || "")
                    .toUpperCase()
                    .replace(/\\s+/g, "")
                    .replace(/[^A-Z0-9/]/g, "");

                const target = normalize(reraNumber);
                const rows = document.querySelectorAll('#compliant_hearing tbody tr');

                for (const row of rows) {
                    const cells = row.querySelectorAll('th, td');
                    if (cells.length >= 10) {
                        const regNum = normalize(cells[3].textContent);

                        if (regNum.includes(target) || target.includes(regNum)) {
                            const link = cells[9].querySelector('a');
                            return link ? link.href : null;
                        }
                    }
                }

                return null;
            }
            """;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private final ReraDetailKeyValueParser keyValueParser;
    private final ScrapeCaptchaDetector captchaDetector;

    @Override
    public boolean supports(ReraSourceCode sourceCode) {
        return ReraSourceCode.HARYANA_RERA == sourceCode;
    }

    @Override
    public ReraRawSearchResult searchByReraNumber(String reraNumber, String captchaText) {
        return scrapeWithPlaywright(reraNumber);
    }

    // -------------------------------------------------------------------------
    // Playwright implementation
    // -------------------------------------------------------------------------

    private ReraRawSearchResult scrapeWithPlaywright(String reraNumber) {
        List<String> warnings = new ArrayList<>();
        String districtValue = resolveDistrictValue(reraNumber, warnings);
        OffsetDateTime fetchedAt = OffsetDateTime.now();

        try (Playwright playwright = Playwright.create()) {

            Browser browser = launchBrowser(playwright);
            try {
                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions()
                                .setViewportSize(1440, 1200)
                                .setUserAgent(USER_AGENT)
                );
                try {
                    Page page = context.newPage();

                    // Step 1 — Open the public search page
                    navigateTo(page, SEARCH_URL);
                    String searchHtml = page.content();

                    if (captchaDetector.detect(searchHtml)) {
                        log.warn("CAPTCHA detected on HRERA search page.");
                        return captchaBlockedResult(page, SEARCH_URL, null, fetchedAt, warnings);
                    }

                    // Step 2 — Select district and submit (form POST, no CAPTCHA)
                    submitDistrictSearch(page, districtValue);

                    page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                            new Page.WaitForLoadStateOptions().setTimeout(45_000));

                    // Step 3 — Wait for result rows to be present in DOM
                    boolean rowsAppeared = waitForSelector(page, RESULTS_ROW_SELECTOR, 20_000, warnings);
                    if (!rowsAppeared) {
                        log.info("No result rows appeared for district={} reraNumber={}", districtValue, reraNumber);
                        return noResultFound(page, SEARCH_URL, fetchedAt, warnings);
                    }

                    // Step 3b — Filter DataTable by RERA number so all matching rows are visible
                    filterResultTableByReraNumber(page, reraNumber, warnings);
                    logVisibleRowsAfterFilter(page, reraNumber);

                    // Step 4 — Find matching row by RERA number (JavaScript search in browser)
                    String detailUrl = evalString(page, JS_FIND_DETAIL_URL, reraNumber, warnings);
                    String formAHUrl = evalString(page, JS_FIND_FORM_AH_URL, reraNumber, warnings);

                    if (detailUrl == null) {
                        log.info("RERA number {} not found in district={} results ({} rows)",
                                reraNumber, districtValue, countRows(page));
                        warnings.add("RERA number not matched in district search results. " +
                                "Searched district: " + districtValue + ". " +
                                "If prefix is unusual, try using sourceCode with a known GGM or PKL registration.");
                        return noResultFound(page, SEARCH_URL, fetchedAt, warnings);
                    }

                    log.info("HRERA match found: reraNumber={} detailUrl={}", reraNumber, detailUrl);

                    // Step 5 — Navigate to detail page, capture HTML + screenshot
                    navigateTo(page, detailUrl);

                    if (captchaDetector.detect(page.content())) {
                        log.warn("CAPTCHA detected on HRERA detail page.");
                        return captchaBlockedResult(page, SEARCH_URL, detailUrl, fetchedAt, warnings);
                    }

                    String detailHtml = page.content();
                    String title      = page.title();
                    String finalUrl   = page.url();
                    byte[] screenshot = captureScreenshot(page, warnings);

                    // Step 6 — Navigate to Form A-H page for richer builder / area data
                    String formAHHtml = "";
                    if (formAHUrl != null) {
                        try {
                            navigateTo(page, formAHUrl);
                            formAHHtml = page.content();
                            log.debug("Form A-H fetched for reraNumber={}", reraNumber);
                        } catch (Exception e) {
                            warnings.add("Form A-H page unavailable: " + e.getMessage());
                            log.warn("Form A-H navigation failed for reraNumber={}: {}", reraNumber, e.getMessage());
                        }
                    }

                    // Step 7 — Combine HTML and extract key-values
                    String combinedHtml = detailHtml + "\n" + formAHHtml;
                    Map<String, String> keyValues = keyValueParser.parse(combinedHtml, detailUrl);
                    log.info("HRERA extracted {} key-value pairs for reraNumber={}", keyValues.size(), reraNumber);

                    return ReraRawSearchResult.builder()
                            .found(!keyValues.isEmpty())
                            .captchaDetected(false)
                            .sourceSearchUrl(SEARCH_URL)
                            .sourceDetailUrl(detailUrl)
                            .finalUrl(finalUrl)
                            .title(title)
                            .html(combinedHtml)
                            .screenshotBytes(screenshot)
                            .extractedKeyValues(keyValues)
                            .fetchedAt(fetchedAt)
                            .warnings(warnings)
                            .build();

                } finally {
                    context.close();
                }
            } finally {
                browser.close();
            }

        } catch (IllegalStateException e) {
            throw e;
        } catch (PlaywrightException e) {
            throw new IllegalStateException(
                    "Playwright error during HRERA scrape: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void filterResultTableByReraNumber(Page page, String reraNumber, List<String> warnings) {
        try {
            Object result = page.evaluate("""
                    (reraNumber) => {
                        const table = document.querySelector('#compliant_hearing');
                        if (!table) {
                            return { success: false, message: "Result table not found" };
                        }

                        // Prefer DataTables API if available
                        if (window.jQuery && window.jQuery.fn && window.jQuery.fn.DataTable) {
                            const dt = window.jQuery('#compliant_hearing').DataTable();

                            dt.search(reraNumber).draw();

                            return {
                                success: true,
                                mode: "DATATABLE_API",
                                searchTerm: reraNumber,
                                filteredCount: dt.rows({ search: 'applied' }).count(),
                                totalCount: dt.rows().count()
                            };
                        }

                        // Fallback: use visible search input
                        const input = document.querySelector('#compliant_hearing_filter input');
                        if (!input) {
                            return { success: false, message: "DataTable search input not found" };
                        }

                        input.value = reraNumber;
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('keyup', { bubbles: true }));

                        return {
                            success: true,
                            mode: "SEARCH_INPUT",
                            searchTerm: reraNumber
                        };
                    }
                    """, reraNumber);

            log.info("HRERA DataTable filtered by RERA number: {}", result);

            page.waitForTimeout(1500);

        } catch (Exception e) {
            warnings.add("Could not filter HRERA result table by RERA number: " + e.getMessage());
            log.warn("HRERA table filter failed for {}: {}", reraNumber, e.getMessage());
        }
    }

    private void logVisibleRowsAfterFilter(Page page, String reraNumber) {
        try {
            Object debug = page.evaluate("""
                    (reraNumber) => {
                        const rows = Array.from(document.querySelectorAll('#compliant_hearing tbody tr'));
                        return {
                            reraNumber,
                            visibleRowCount: rows.length,
                            bodyContainsRera: document.body.innerText.toUpperCase().includes(reraNumber.toUpperCase()),
                            rows: rows.slice(0, 5).map((row, rowIndex) => ({
                                rowIndex,
                                cells: Array.from(row.querySelectorAll('th,td')).map((cell, cellIndex) => ({
                                    cellIndex,
                                    text: cell.textContent.trim(),
                                    links: Array.from(cell.querySelectorAll('a')).map(a => a.href)
                                }))
                            }))
                        };
                    }
                    """, reraNumber);

            log.info("HRERA visible rows after filter: {}", debug);
        } catch (Exception e) {
            log.warn("HRERA visible row debug failed: {}", e.getMessage());
        }
    }

    private void submitDistrictSearch(Page page, String districtValue) {
        try {
            Object result = page.evaluate("""
                    (districtValue) => {
                        const select = document.querySelector("select[name='district']");
                        if (!select) {
                            throw new Error("District select not found");
                        }

                        const options = Array.from(select.options)
                            .map(o => ({ value: o.value, text: o.textContent.trim() }));

                        const option = Array.from(select.options)
                            .find(o => o.value === districtValue);

                        if (!option) {
                            throw new Error("District option not found: " + districtValue +
                                ". Available options: " + JSON.stringify(options));
                        }

                        select.value = districtValue;
                        select.dispatchEvent(new Event("input", { bubbles: true }));
                        select.dispatchEvent(new Event("change", { bubbles: true }));

                        if (window.jQuery) {
                            window.jQuery(select).val(districtValue).trigger("change");
                        }

                        const form = document.querySelector("form[name='search_form']");
                        if (!form) {
                            throw new Error("HRERA search form not found");
                        }

                        if (!form.querySelector("input[name='basic_search']")) {
                            const hiddenSubmit = document.createElement("input");
                            hiddenSubmit.type = "hidden";
                            hiddenSubmit.name = "basic_search";
                            hiddenSubmit.value = "Search";
                            form.appendChild(hiddenSubmit);
                        }

                        form.submit();

                        return {
                            selectedValue: select.value,
                            selectedText: option.textContent.trim(),
                            formAction: form.action,
                            formMethod: form.method,
                            selectVisible: !!(select.offsetWidth || select.offsetHeight || select.getClientRects().length)
                        };
                    }
                    """, districtValue);

            log.info("HRERA district form submitted via JS: {}", result);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to submit HRERA district search for value=" + districtValue + ": " + e.getMessage(),
                    e
            );
        }
    }

    private String resolveDistrictValue(String reraNumber, List<String> warnings) {
        if (reraNumber == null || !reraNumber.contains("/")) {
            warnings.add("RERA number has no '/' separator — falling back to all-districts search");
            return "999";
        }
        String prefix = reraNumber.substring(0, reraNumber.indexOf('/')).toUpperCase().trim();
        String value  = RERA_PREFIX_TO_DISTRICT.get(prefix);
        if (value == null) {
            warnings.add("Unknown RERA prefix '" + prefix +
                    "' — searching all districts. This may be slower.");
            return "999";
        }
        return value;
    }

    private String evalString(Page page, String jsExpression, String arg, List<String> warnings) {
        try {
            Object result = page.evaluate(jsExpression, arg);
            return result instanceof String s ? s : null;
        } catch (Exception e) {
            warnings.add("JavaScript evaluation failed: " + e.getMessage());
            log.warn("page.evaluate failed: {}", e.getMessage());
            return null;
        }
    }

    private long countRows(Page page) {
        try {
            Object count = page.evaluate(
                    "() => document.querySelectorAll('#compliant_hearing tbody tr').length");
            return count instanceof Number n ? n.longValue() : -1;
        } catch (Exception e) {
            return -1;
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
                    "-Dexec.args=\"install chromium\"", e
            );
        }
    }

    private void navigateTo(Page page, String url) {
        try {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(60_000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(15_000));
        } catch (TimeoutError e) {
            throw new IllegalStateException("Timeout navigating to: " + url);
        }
    }

    private boolean waitForSelector(Page page, String selector, int timeoutMs,
                                    List<String> warnings) {
        try {
            page.waitForSelector(selector,
                    new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
            return true;
        } catch (TimeoutError e) {
            warnings.add("Selector not found within " + timeoutMs + "ms: " + selector);
            return false;
        }
    }

    private byte[] captureScreenshot(Page page, List<String> warnings) {
        try {
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (Exception e) {
            warnings.add("Screenshot capture failed: " + e.getMessage());
            return null;
        }
    }

    private ReraRawSearchResult captchaBlockedResult(Page page, String searchUrl,
                                                      String detailUrl,
                                                      OffsetDateTime fetchedAt,
                                                      List<String> warnings) {
        warnings.add("CAPTCHA or anti-bot challenge detected. Scrape aborted.");
        byte[] screenshot = captureScreenshot(page, warnings);
        return ReraRawSearchResult.builder()
                .found(false)
                .captchaDetected(true)
                .sourceSearchUrl(searchUrl)
                .sourceDetailUrl(detailUrl)
                .finalUrl(page.url())
                .title(page.title())
                .html(page.content())
                .screenshotBytes(screenshot)
                .extractedKeyValues(Map.of())
                .fetchedAt(fetchedAt)
                .warnings(warnings)
                .build();
    }

    private ReraRawSearchResult noResultFound(Page page, String searchUrl,
                                               OffsetDateTime fetchedAt,
                                               List<String> warnings) {
        byte[] screenshot = captureScreenshot(page, warnings);
        return ReraRawSearchResult.builder()
                .found(false)
                .captchaDetected(false)
                .sourceSearchUrl(searchUrl)
                .sourceDetailUrl(null)
                .finalUrl(page.url())
                .title(page.title())
                .html(page.content())
                .screenshotBytes(screenshot)
                .extractedKeyValues(Map.of())
                .fetchedAt(fetchedAt)
                .warnings(warnings)
                .build();
    }
}
