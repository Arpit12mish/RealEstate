package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AmenitySuggestionResponse {
    private String amenityCode;
    private String amenityLabel;
    private ProjectAmenityCategory category;
    private String categoryLabel;
    private String iconKey;
    private boolean rare;
}
