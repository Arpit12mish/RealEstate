package com.brandPitara.sfs.dashboard.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectConstructionStageCode;
import com.brandPitara.sfs.projectmeter.enums.ProjectStageStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DashboardProjectConstructionStageRequest {

    @NotNull
    private ProjectConstructionStageCode stageCode;

    @NotBlank
    @Size(max = 120)
    private String stageLabel;

    @NotNull
    @Min(0) @Max(999)
    private Integer displayOrder;

    @NotNull
    @Min(0) @Max(value = 100, message ="weightPercent must be between 0 and 100")
    private Integer weightPercent;

    @Min(0) @Max(value = 100, message ="progressPercent must be between 0 and 100")
    private Integer progressPercent;

    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    private ProjectStageStatus status;

    @Size(max = 500)
    private String remarks;

    @PositiveOrZero
    private Integer evidenceCount;
    private Boolean verified;
}