package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTimelineResponse {
    private LocalDate originalCompletionDate;
    private LocalDate latestReraCompletionDate;
    private LocalDate actualCompletionDate;
    private Integer reraExtensionCount;
    private Integer delayVsOriginalDays;
    private Integer delayVsLatestReraDays;
    private String timelineStatus;
    private String timelineLabel;
    private String timelineHint;
}
