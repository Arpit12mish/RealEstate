package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AmenityCategoryMetaResponse {
    private ProjectAmenityCategory value;
    private String label;
    private int displayOrder;
}
