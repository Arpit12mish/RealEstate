package com.brandPitara.sfs.dashboard.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardProjectAmenityRequest {

    @NotBlank
    @Size(max = 80)
    private String amenityCode;

    @NotBlank
    @Size(max = 120)
    private String amenityLabel;

    @NotNull
    private ProjectAmenityStatus status;

    @Min(0) @Max(value = 100, message ="progressPercent must be between 0 and 100")
    private Integer progressPercent;

    @Min(0) @Max(value = 100, message ="weightPercent must be between 0 and 100")
    private Integer weightPercent;

    @NotNull
    @Min(0) @Max(999)
    private Integer displayOrder;

    @Size(max = 500)
    private String remarks;
    private Boolean verified;
}