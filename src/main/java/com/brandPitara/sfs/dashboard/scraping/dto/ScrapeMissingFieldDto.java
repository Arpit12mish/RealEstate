package com.brandPitara.sfs.dashboard.scraping.dto;

import com.brandPitara.sfs.dashboard.scraping.enums.ScrapeFieldSection;
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
public class ScrapeMissingFieldDto {

    private ScrapeFieldSection section;
    private String fieldKey;
    private String fieldLabel;
    private String reason;
}
