package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.project.enums.ProjectMediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterMediaItemResponse {
    private Long id;
    private ProjectMediaType mediaType;
    private String url;
    private String thumbnailUrl;
    private String title;
    private String caption;
    private Integer displayOrder;
    private Boolean cover;
}
