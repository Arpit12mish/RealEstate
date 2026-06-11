package com.brandPitara.sfs.dashboard.projectmeter.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class DashboardProjectTimelineRequest {
    private LocalDate originalCompletionDate;
    private LocalDate latestReraCompletionDate;
    private LocalDate actualCompletionDate;

    @Min(value = 0, message = "reraExtensionCount must be 0 or greater")
    private Integer reraExtensionCount;
}
