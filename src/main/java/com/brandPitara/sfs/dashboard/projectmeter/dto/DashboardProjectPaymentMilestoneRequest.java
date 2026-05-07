package com.brandPitara.sfs.dashboard.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectConstructionStageCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardProjectPaymentMilestoneRequest {

    @NotBlank
    @Size(max = 80)
    private String milestoneCode;

    @NotBlank
    @Size(max = 180)
    private String milestoneLabel;

    @Size(max = 1000)
    private String description;

    @NotNull
    @Min(0) @Max(value = 100, message ="percentageValue must be between 0 and 100")
    private Integer percentageValue;

    @NotNull
    @Min(0) @Max(999)
    private Integer displayOrder;

    private ProjectConstructionStageCode linkedStageCode;

    private Boolean active;
}