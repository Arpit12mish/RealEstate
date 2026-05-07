package com.brandPitara.sfs.dashboard.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardProjectComplianceItemRequest {

    @NotNull
    private ProjectComplianceGroup itemGroup;

    @NotBlank
    @Size(max = 100)
    private String itemKey;

    @NotBlank
    @Size(max = 180)
    private String itemLabel;

    @NotNull
    private ProjectComplianceStatus status;

    @Size(max = 300)
    private String valueText;

    @Size(max = 500)
    private String documentUrl;

    @Size(max = 500)
    private String remarks;

    @NotNull
    @Min(0) @Max(999)
    private Integer displayOrder;

    private Boolean verified;
}