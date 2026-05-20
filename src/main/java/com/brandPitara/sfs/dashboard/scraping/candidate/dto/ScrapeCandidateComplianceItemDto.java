package com.brandPitara.sfs.dashboard.scraping.candidate.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScrapeCandidateComplianceItemDto {

    private Long id;
    private String itemGroup;
    private String itemKey;
    private String itemLabel;
    private String status;
    private String valueText;
    private String documentUrl;
    private String remarks;
    private int displayOrder;
    private boolean verified;
}
