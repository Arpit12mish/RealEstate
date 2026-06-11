package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterCardResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardMobilePreviewResponse {

    private DashboardMobilePreviewMeta meta;
    private ProjectMeterCardResponse card;
    private ProjectResponse detail;
    private ProjectMeterDetailResponse meter;
}
