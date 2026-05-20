package com.brandPitara.sfs.dashboard.scraping.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Candidate compliance item extracted from a RERA portal scrape.
 * Fields align with DashboardProjectComplianceItemRequest.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScrapedComplianceCandidateDto {

    private ProjectComplianceGroup itemGroup;
    private String itemKey;
    private String itemLabel;
    private ProjectComplianceStatus status;
    private String valueText;
    private String documentUrl;
    private String remarks;
    private Integer displayOrder;
    private Boolean verified;
}
