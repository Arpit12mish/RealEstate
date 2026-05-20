package com.brandPitara.sfs.dashboard.scraping.provider;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PlaywrightScrapingProvider implements ScrapingProvider {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final int NETWORK_IDLE_TIMEOUT_MS = 15_000;

    @Override
    public ScrapeProviderResult fetchRenderedPage(ScrapeProviderRequest request) {
        List<String> warnings = new ArrayList<>();

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
                    OffsetDateTime fetchedAt = OffsetDateTime.now();

                    navigateSafely(page, request.url(), request.timeoutMs());

                    waitForNetworkIdleOrFallback(page, request.url(), warnings);

                    String finalUrl = page.url();
                    String title = page.title();
                    String html = page.content();

                    log.info("Playwright fetched url={} finalUrl={} htmlLen={}",
                            request.url(), finalUrl, html != null ? html.length() : 0);

                    byte[] screenshotBytes = captureScreenshot(page, request, request.url(), warnings);

                    return ScrapeProviderResult.builder()
                            .requestedUrl(request.url())
                            .finalUrl(finalUrl)
                            .title(title)
                            .html(html)
                            .screenshotBytes(screenshotBytes)
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
                    "Playwright encountered an unexpected error: " + e.getMessage(), e
            );
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
                    "Please install Chromium by running: " +
                    "mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI " +
                    "-Dexec.args=\"install chromium\"",
                    e
            );
        }
    }

    private void navigateSafely(Page page, String url, int timeoutMs) {
        try {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(timeoutMs)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        } catch (TimeoutError e) {
            throw new IllegalStateException("Scrape navigation timed out for URL: " + url);
        }
    }

    private void waitForNetworkIdleOrFallback(Page page, String url, List<String> warnings) {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(NETWORK_IDLE_TIMEOUT_MS));
        } catch (TimeoutError e) {
            String msg = "NETWORKIDLE wait timed out; content captured at DOMCONTENTLOADED state.";
            warnings.add(msg);
            log.warn("NETWORKIDLE timed out for url={}", url);
        }
    }

    private byte[] captureScreenshot(Page page, ScrapeProviderRequest request,
                                     String url, List<String> warnings) {
        if (!request.fullPageScreenshot()) {
            return null;
        }
        try {
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (Exception e) {
            String msg = "Screenshot capture failed: " + e.getMessage();
            warnings.add(msg);
            log.warn("Screenshot failed for url={}: {}", url, e.getMessage());
            return null;
        }
    }
}
