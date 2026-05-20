package com.brandPitara.sfs.dashboard.scraping.provider;

public record ScrapeProviderRequest(
        String url,
        int timeoutMs,
        boolean fullPageScreenshot
) {}
