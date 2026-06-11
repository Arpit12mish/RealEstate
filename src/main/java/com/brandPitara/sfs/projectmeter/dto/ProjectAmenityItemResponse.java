package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.projectmeter.enums.ProjectAmenityCategory;
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
    private ProjectAmenityCategory category;
    private String categoryLabel;
    private String iconKey;
    private Boolean rare;
    private Boolean available;
    private ProjectAmenityStatus status;
    private Integer progressPercent;
    private Integer weightPercent;
    private Integer displayOrder;
    private Integer categoryDisplayOrder;
    private String remarks;
    private Boolean verified;
    private Boolean publicVisible;
    private Boolean active;
}
