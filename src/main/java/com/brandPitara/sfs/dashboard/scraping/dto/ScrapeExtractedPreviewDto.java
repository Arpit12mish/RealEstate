package com.brandPitara.sfs.dashboard.scraping.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScrapeExtractedPreviewDto {

    private String projectName;
    private String builderName;
    private String reraNumber;
    private String cityName;
    private String statusText;
    private String sourceUrl;
    private Map<String, Object> raw;
}
