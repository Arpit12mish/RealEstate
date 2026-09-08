package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectComplianceGroupResponse {
    private String group;
    private String groupLabel;
    private List<ProjectComplianceItemResponse> items;
}
