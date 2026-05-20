package com.brandPitara.sfs.dashboard.scraping.provider;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class ScrapeProviderResult {

    private final String requestedUrl;
    private final String finalUrl;
    private final String title;
    private final String html;
    private final byte[] screenshotBytes;
    private final OffsetDateTime fetchedAt;

    @Builder.Default
    private final List<String> warnings = new ArrayList<>();
}
