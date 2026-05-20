package com.brandPitara.sfs.dashboard.scraping.provider;

/**
 * Abstraction over a rendered-page fetch strategy.
 *
 * Phase S1 implementation: PlaywrightScrapingProvider (local Chromium).
 * Future implementations can swap in Apify, Zyte, ScrapingBee, or BrightData
 * by adding a new @Component that implements this interface and using
 * @Primary or @Qualifier to select it.
 */
public interface ScrapingProvider {

    ScrapeProviderResult fetchRenderedPage(ScrapeProviderRequest request);
}
