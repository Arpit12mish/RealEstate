package com.brandPitara.sfs.dashboard.scraping.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class LocalScrapeFileStorageService implements ScrapeFileStorageService {

    private static final Path BASE_DIR = Path.of("/tmp/sfs-scrapes");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(BASE_DIR);
            log.info("Scrape file storage directory ready: {}", BASE_DIR.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create scrape storage directory: {}", BASE_DIR, e);
        }
    }

    @Override
    public SavedScrapeFile saveHtml(String filePrefix, String html) {
        String fileName = filePrefix + ".html";
        Path filePath = BASE_DIR.resolve(fileName);
        try {
            Files.writeString(filePath, html, StandardCharsets.UTF_8);
            long size = Files.size(filePath);
            log.debug("Saved HTML file: {} ({} bytes)", filePath, size);
            return new SavedScrapeFile(filePath.toAbsolutePath().toString(), fileName, size);
        } catch (IOException e) {
            log.error("Failed to save HTML scrape file: {}", fileName, e);
            throw new IllegalStateException("Failed to save scrape HTML file: " + fileName);
        }
    }

    @Override
    public SavedScrapeFile saveScreenshot(String filePrefix, byte[] bytes) {
        String fileName = filePrefix + ".png";
        Path filePath = BASE_DIR.resolve(fileName);
        try {
            Files.write(filePath, bytes);
            long size = Files.size(filePath);
            log.debug("Saved screenshot: {} ({} bytes)", filePath, size);
            return new SavedScrapeFile(filePath.toAbsolutePath().toString(), fileName, size);
        } catch (IOException e) {
            log.error("Failed to save screenshot file: {}", fileName, e);
            throw new IllegalStateException("Failed to save scrape screenshot file: " + fileName);
        }
    }
}
