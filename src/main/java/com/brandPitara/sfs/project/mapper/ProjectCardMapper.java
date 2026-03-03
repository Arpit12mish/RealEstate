package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectCardDto;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;

import java.util.List;

public class ProjectCardMapper {

  public static ProjectCardDto toCard(ProjectEntity e, List<ProjectMediaEntity> media) {
    var picked = ProjectMediaPicker.pick(media, false);

    return ProjectCardDto.builder()
        .id(e.getId())
        .name(e.getName())
        .cityId(e.getCity() != null ? e.getCity().getId() : null)
        .cityName(e.getCity() != null ? e.getCity().getName() : null)
        .addressLine(e.getAddressLine())
        .priceMin(e.getPriceMin())
        .priceMax(e.getPriceMax())
        .builderId(e.getBuilder() != null ? e.getBuilder().getId() : null)
        .builderName(e.getBuilder() != null ? e.getBuilder().getName() : null)
        .builderLogoUrl(e.getBuilder() != null ? e.getBuilder().getLogoUrl() : null)
        .coverMediaUrl(picked.coverMediaUrl())
        .coverMediaType(picked.coverMediaType())
        .propertyTypes(e.getPropertyTypes())
        .build();
  }
}
