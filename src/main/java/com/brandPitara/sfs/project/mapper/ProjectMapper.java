package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;

import java.util.List;

public class ProjectMapper {

  public static ProjectResponse toResponse(ProjectEntity e) {
    return ProjectResponse.builder()
        .id(e.getId())
        .builderId(e.getBuilder() != null ? e.getBuilder().getId() : null)
        .builderName(e.getBuilder() != null ? e.getBuilder().getName() : null)
        .builderLogoUrl(e.getBuilder() != null ? e.getBuilder().getLogoUrl() : null)

        .name(e.getName())
        .slug(e.getSlug())
        .description(e.getDescription())

        .cityId(e.getCity() != null ? e.getCity().getId() : null)
        .cityName(e.getCity() != null ? e.getCity().getName() : null)

        .addressLine(e.getAddressLine())
        .latitude(e.getLatitude())
        .longitude(e.getLongitude())

        .priceMin(e.getPriceMin())
        .priceMax(e.getPriceMax())
        .possessionDate(e.getPossessionDate())
        .reraNumber(e.getReraNumber())

        .status(e.getStatus())
        .propertyTypes(e.getPropertyTypes())

        .active(Boolean.TRUE.equals(e.getActive()))
        .published(Boolean.TRUE.equals(e.getPublished()))
        .priority(e.getPriority() != null ? e.getPriority() : 0)
        .build();
  }

  // ✅ NEW: enrich response with cover/brochure without breaking existing calls
  public static ProjectResponse toResponse(ProjectEntity entity, List<ProjectMediaEntity> media) {
    ProjectResponse res = toResponse(entity);

    var picked = ProjectMediaPicker.pick(media);

    res.setCoverMediaUrl(picked.coverMediaUrl());
    res.setCoverMediaType(picked.coverMediaType());
    res.setBrochureUrl(picked.brochureUrl());
    res.setHasImages(picked.hasImages());
    res.setHasVideo(picked.hasVideo());

    return res;
  }
}
