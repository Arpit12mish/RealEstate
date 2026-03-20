package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPaymentMilestoneResponse {
    private Long id;
    private String milestoneCode;
    private String milestoneLabel;
    private String description;
    private Integer percentageValue;
    private String linkedStageCode;
}