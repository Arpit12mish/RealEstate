package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAmenityGroupResponse {
    private ProjectAmenityCategory category;
    private String categoryLabel;
    private Integer displayOrder;
    private List<ProjectAmenityItemResponse> items;
}
