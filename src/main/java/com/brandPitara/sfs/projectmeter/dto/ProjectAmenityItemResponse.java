package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectAmenityItemResponse {
    private Long id;
    private String amenityCode;
    private String amenityLabel;
    private ProjectAmenityStatus status;
    private Integer progressPercent;
    private Integer weightPercent;
    private String remarks;
    private Boolean verified;
}