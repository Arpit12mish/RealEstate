package com.brandPitara.sfs.dashboard.scraping.dto;

import com.brandPitara.sfs.dashboard.scraping.enums.ScrapeFieldSection;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScrapeFieldResultDto {

    private ScrapeFieldSection section;
    private String fieldKey;
    private String fieldLabel;
    private boolean found;
    private Object value;
    private String sourceLabel;
    private Integer confidence;
    private String reason;
}
