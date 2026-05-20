package com.brandPitara.sfs.dashboard.scraping.service.impl;

import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeExtractedPreviewDto;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeTestUrlRequest;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeTestUrlResponse;
import com.brandPitara.sfs.dashboard.scraping.parser.ReraPreviewParser;
import com.brandPitara.sfs.dashboard.scraping.provider.ScrapeProviderRequest;
import com.brandPitara.sfs.dashboard.scraping.provider.ScrapeProviderResult;
import com.brandPitara.sfs.dashboard.scraping.provider.ScrapingProvider;
import com.brandPitara.sfs.dashboard.scraping.service.DashboardScrapingTestService;
import com.brandPitara.sfs.dashboard.scraping.util.SavedScrapeFile;
import com.brandPitara.sfs.dashboard.scraping.util.ScrapeCaptchaDetector;
import com.brandPitara.sfs.dashboard.scraping.util.ScrapeFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardScrapingTestServiceImpl implements DashboardScrapingTestService {

    private static final int DEFAULT_TIMEOUT_MS = 60_000;
    private static final DateTimeFormatter PREFIX_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ScrapingProvider scrapingProvider;
    private final ScrapeFileStorageService fileStorageService;
    private final ReraPreviewParser reraPreviewParser;
    private final ScrapeCaptchaDetector captchaDetector;

    @Override
    public ScrapeTestUrlResponse testUrl(ScrapeTestUrlRequest request) {
        validateUrlSafety(request.getUrl());

        String sessionPrefix = generateSessionPrefix();
        List<String> warnings = new ArrayList<>();

        ScrapeProviderResult result = scrapingProvider.fetchRenderedPage(
                new ScrapeProviderRequest(request.getUrl(), DEFAULT_TIMEOUT_MS, true)
        );

        warnings.addAll(result.getWarnings());

        String html = result.getHtml();

        String rawHtmlPath = null;
        boolean rawHtmlSaved = false;
        if (html != null && !html.isBlank()) {
            try {
                SavedScrapeFile saved = fileStorageService.saveHtml(sessionPrefix, html);
                rawHtmlPath = saved.path();
                rawHtmlSaved = true;
            } catch (Exception e) {
                warnings.add("Failed to save raw HTML: " + e.getMessage());
                log.warn("HTML save failed for prefix={}: {}", sessionPrefix, e.getMessage());
            }
        }

        String screenshotPath = null;
        boolean screenshotSaved = false;
        byte[] screenshotBytes = result.getScreenshotBytes();
        if (screenshotBytes != null && screenshotBytes.length > 0) {
            try {
                SavedScrapeFile saved = fileStorageService.saveScreenshot(sessionPrefix, screenshotBytes);
                screenshotPath = saved.path();
                screenshotSaved = true;
            } catch (Exception e) {
                warnings.add("Failed to save screenshot: " + e.getMessage());
                log.warn("Screenshot save failed for prefix={}: {}", sessionPrefix, e.getMessage());
            }
        }

        boolean captchaDetected = captchaDetector.detect(html);
        if (captchaDetected) {
            warnings.add("CAPTCHA or anti-bot signal detected in rendered HTML.");
        }

        List<ScrapeExtractedPreviewDto> preview = List.of();
        if (html != null && !html.isBlank()) {
            try {
                preview = reraPreviewParser.parse(html, result.getFinalUrl());
            } catch (Exception e) {
                warnings.add("HTML parsing failed: " + e.getMessage());
                log.warn("Parser failed for url={}: {}", result.getFinalUrl(), e.getMessage());
            }
        }

        return ScrapeTestUrlResponse.builder()
                .requestedUrl(result.getRequestedUrl())
                .finalUrl(result.getFinalUrl())
                .title(result.getTitle())
                .htmlLength(html != null ? html.length() : 0)
                .captchaDetected(captchaDetected)
                .rawHtmlSaved(rawHtmlSaved)
                .screenshotSaved(screenshotSaved)
                .rawHtmlPath(rawHtmlPath)
                .screenshotPath(screenshotPath)
                .extractedPreview(preview)
                .warnings(warnings.isEmpty() ? null : warnings)
                .fetchedAt(result.getFetchedAt())
                .build();
    }

    private void validateUrlSafety(String rawUrl) {
        URL parsed;
        try {
            parsed = new URL(rawUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + rawUrl);
        }

        String protocol = parsed.getProtocol();
        if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("Only http and https protocols are allowed.");
        }

        String host = parsed.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL is missing a valid host.");
        }

        // Block well-known internal hostnames before DNS resolution
        String lowerHost = host.toLowerCase();
        if (lowerHost.equals("localhost")
                || lowerHost.equals("127.0.0.1")
                || lowerHost.equals("0.0.0.0")
                || lowerHost.startsWith("169.254.")       // link-local
                || lowerHost.startsWith("10.")            // RFC-1918 class A
                || lowerHost.startsWith("192.168.")) {   // RFC-1918 class C
            throw new IllegalArgumentException(
                    "Scraping internal or private-network URLs is not allowed."
            );
        }
        // RFC-1918 class B: 172.16.0.0 – 172.31.255.255
        if (lowerHost.matches("172\\.(1[6-9]|2\\d|3[01])\\..*")) {
            throw new IllegalArgumentException(
                    "Scraping internal or private-network URLs is not allowed."
            );
        }

        // DNS-resolution SSRF guard
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addr.isLoopbackAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isAnyLocalAddress()) {
                    throw new IllegalArgumentException(
                            "URL resolves to a private or loopback address, which is not allowed."
                    );
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host);
        }
    }

    private String generateSessionPrefix() {
        String timestamp = LocalDateTime.now().format(PREFIX_FORMATTER);
        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "scrape-test-" + timestamp + "-" + shortId;
    }
}
