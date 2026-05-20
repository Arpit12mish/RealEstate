package com.brandPitara.sfs.dashboard.scraping.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScrapeTestUrlResponse {

    private String requestedUrl;
    private String finalUrl;
    private String title;
    private Integer htmlLength;
    private Boolean captchaDetected;
    private Boolean rawHtmlSaved;
    private Boolean screenshotSaved;
    private String rawHtmlPath;
    private String screenshotPath;
    private List<ScrapeExtractedPreviewDto> extractedPreview;
    private List<String> warnings;
    private OffsetDateTime fetchedAt;
}
