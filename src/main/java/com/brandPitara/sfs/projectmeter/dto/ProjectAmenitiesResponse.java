package com.brandPitara.sfs.projectmeter.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAmenitiesResponse {
    private Integer completionPercent;
    private List<ProjectAmenityItemResponse> items;
}