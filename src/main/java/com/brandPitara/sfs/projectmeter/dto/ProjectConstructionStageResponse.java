package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectConstructionStageCode;
import com.brandPitara.sfs.projectmeter.enums.ProjectStageStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConstructionStageResponse {
    private Long id;
    private ProjectConstructionStageCode stageCode;
    private String stageLabel;
    private Integer displayOrder;
    private Integer weightPercent;
    private Integer progressPercent;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private ProjectStageStatus status;
    private String remarks;
    private Integer evidenceCount;
    private Boolean verified;
}