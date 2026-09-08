package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectFloorPlanVisualAnalysisResponse;
import com.brandPitara.sfs.project.dto.VisualAnalysisTagResponse;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisTagEntity;

public class ProjectFloorPlanVisualAnalysisMapper {

  public static ProjectFloorPlanVisualAnalysisResponse toResponse(ProjectFloorPlanVisualAnalysisEntity e) {
    return ProjectFloorPlanVisualAnalysisResponse.builder()
        .title(e.getTitle())
        .description(e.getDescription())
        .mediaType(e.getMediaType())
        .mediaUrl(e.getMediaUrl())
        .tags(e.getTags().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .map(ProjectFloorPlanVisualAnalysisMapper::toTagResponse)
            .toList())
        .build();
  }

  private static VisualAnalysisTagResponse toTagResponse(ProjectFloorPlanVisualAnalysisTagEntity t) {
    return VisualAnalysisTagResponse.builder()
        .label(t.getLabel())
        .color(t.getColor())
        .sortOrder(t.getSortOrder() != null ? t.getSortOrder() : 0)
        .build();
  }
}
