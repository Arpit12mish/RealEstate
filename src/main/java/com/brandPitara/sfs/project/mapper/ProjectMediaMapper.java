package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;

public class ProjectMediaMapper {

  public static ProjectMediaResponse toResponse(ProjectMediaEntity e) {
    return ProjectMediaResponse.builder()
        .id(e.getId())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .mediaType(e.getMediaType())
        .url(e.getUrl())
        .caption(e.getCaption())
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }
}
