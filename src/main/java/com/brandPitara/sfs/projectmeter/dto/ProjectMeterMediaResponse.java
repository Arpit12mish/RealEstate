package com.brandPitara.sfs.projectmeter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterMediaResponse {
    private String coverImageUrl;

    @Builder.Default
    private List<ProjectMeterMediaItemResponse> items = List.of();
}
