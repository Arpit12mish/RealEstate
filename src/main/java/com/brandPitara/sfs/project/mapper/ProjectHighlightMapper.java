package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectHighlightResponse;
import com.brandPitara.sfs.project.entity.ProjectHighlightEntity;

public class ProjectHighlightMapper {

  public static ProjectHighlightResponse toResponse(ProjectHighlightEntity e) {
    return ProjectHighlightResponse.builder()
        .id(e.getId())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .title(e.getTitle())
        .subtitle(e.getSubtitle())
        .iconKey(e.getIconKey())
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }
}