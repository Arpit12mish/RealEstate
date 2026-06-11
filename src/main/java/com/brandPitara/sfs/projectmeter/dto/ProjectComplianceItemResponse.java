package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectComplianceItemResponse {
    private Long id;
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