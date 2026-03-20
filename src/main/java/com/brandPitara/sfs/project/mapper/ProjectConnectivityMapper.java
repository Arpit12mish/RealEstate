package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectConnectivityPlaceResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.entity.ProjectConnectivityEntity;
import com.brandPitara.sfs.project.entity.ProjectConnectivityPlaceEntity;

import java.util.List;

public class ProjectConnectivityMapper {

  public static ProjectConnectivityPlaceResponse toPlaceResponse(ProjectConnectivityPlaceEntity e) {
    return ProjectConnectivityPlaceResponse.builder()
        .id(e.getId())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .placeName(e.getPlaceName())
        .placeType(e.getPlaceType())
        .distanceLabel(e.getDistanceLabel())
        .imageUrl(e.getImageUrl())
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  public static ProjectConnectivityResponse toResponse(
      ProjectConnectivityEntity connectivity,
      Long projectId,
      List<ProjectConnectivityPlaceEntity> places
  ) {
    return ProjectConnectivityResponse.builder()
        .projectId(projectId)
        .title(connectivity != null ? connectivity.getTitle() : null)
        .subtitle(connectivity != null ? connectivity.getSubtitle() : null)
        .mapImageUrl(connectivity != null ? connectivity.getMapImageUrl() : null)
        .places(places.stream().map(ProjectConnectivityMapper::toPlaceResponse).toList())
        .build();
  }
}