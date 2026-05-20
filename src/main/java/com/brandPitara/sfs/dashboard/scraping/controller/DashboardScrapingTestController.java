package com.brandPitara.sfs.dashboard.scraping.controller;

import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeTestUrlRequest;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeTestUrlResponse;
import com.brandPitara.sfs.dashboard.scraping.service.DashboardScrapingTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/scraping")
@RequiredArgsConstructor
public class DashboardScrapingTestController {

    private final DashboardScrapingTestService scrapingTestService;

    @PostMapping("/test-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScrapeTestUrlResponse> testUrl(
            @Valid @RequestBody ScrapeTestUrlRequest request
    ) {
        return ResponseEntity.ok(scrapingTestService.testUrl(request));
    }
}
