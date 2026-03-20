package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterSummaryResponse {
    private Long projectId;
    private Integer constructionProgressPercent;
    private Integer delayDays;
    private LocalDate constructionStartDate;
    private LocalDate expectedCompletionDate;
    private LocalDate revisedCompletionDate;
    private Boolean verified;
    private OffsetDateTime computedAt;
    private OffsetDateTime lastVerifiedAt;
}