package com.brandPitara.sfs.dashboard.projectmeter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardProjectMeterWriteResponse {

    private Long projectId;
    private String section;
    private String message;
}