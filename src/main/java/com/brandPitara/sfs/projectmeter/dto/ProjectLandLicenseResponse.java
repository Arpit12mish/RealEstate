package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectLandLicenseResponse {
    private List<ProjectComplianceItemResponse> items;
}