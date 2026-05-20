package com.brandPitara.sfs.dashboard.scraping.service;

import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeTestUrlRequest;
import com.brandPitara.sfs.dashboard.scraping.dto.ScrapeTestUrlResponse;

public interface DashboardScrapingTestService {

    ScrapeTestUrlResponse testUrl(ScrapeTestUrlRequest request);
}
