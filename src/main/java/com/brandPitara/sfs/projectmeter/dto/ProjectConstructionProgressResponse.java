package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConstructionProgressResponse {
    private Long projectId;
    private Integer overallProgressPercent;
    private Integer delayDays;
    private ProjectTimelineResponse timeline;
    private List<ProjectConstructionStageResponse> stages;
}
