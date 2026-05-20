package com.brandPitara.sfs.dashboard.scraping.util;

public interface ScrapeFileStorageService {

    SavedScrapeFile saveHtml(String filePrefix, String html);

    SavedScrapeFile saveScreenshot(String filePrefix, byte[] bytes);
}
