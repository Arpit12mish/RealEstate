package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.PropertyType;

import java.util.List;
import java.util.Set;

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
        .monthlyEmiMin(e.getMonthlyEmiMin())
        .monthlyEmiMax(e.getMonthlyEmiMax())
        .averagePricePerSqft(e.getAveragePricePerSqft())
        .startDate(e.getStartDate())
        .possessionDate(e.getPossessionDate())
        .reraNumber(e.getReraNumber())

        .status(e.getStatus())
        .reviewStatus(e.getReviewStatus())
        .propertyTypes(copyPropertyTypes(e))

        .active(Boolean.TRUE.equals(e.getActive()))
        .published(Boolean.TRUE.equals(e.getPublished()))
        .priority(e.getPriority() != null ? e.getPriority() : 0)
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }

  // ✅ NEW: enrich response with cover/brochure without breaking existing calls
  public static ProjectResponse toResponse(ProjectEntity entity, List<ProjectMediaEntity> media) {
    ProjectResponse res = toResponse(entity);

    var picked = ProjectMediaPicker.pick(media, true); // will throw if inconsistent, false: dont break

    res.setCoverMediaUrl(picked.coverMediaUrl());
    res.setCoverMediaType(picked.coverMediaType());
    res.setBrochureUrl(picked.brochureUrl());
    res.setHasImages(picked.hasImages());
    res.setHasVideo(picked.hasVideo());

    return res;
  }

  // Public-safe mapping — strips admin/internal fields (reviewStatus, active, published, priority, timestamps)
  public static ProjectPublicResponse toPublicResponse(ProjectEntity e) {
    return ProjectPublicResponse.builder()
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
        .monthlyEmiMin(e.getMonthlyEmiMin())
        .monthlyEmiMax(e.getMonthlyEmiMax())
        .averagePricePerSqft(e.getAveragePricePerSqft())
        .startDate(e.getStartDate())
        .possessionDate(e.getPossessionDate())
        .reraNumber(e.getReraNumber())

        .status(e.getStatus())
        .propertyTypes(copyPropertyTypes(e))
        .build();
  }

  private static Set<PropertyType> copyPropertyTypes(ProjectEntity project) {
    if (project.getPropertyTypes() == null || project.getPropertyTypes().isEmpty()) {
      return Set.of();
    }

    return Set.copyOf(project.getPropertyTypes());
  }

  public static ProjectPublicResponse toPublicResponse(ProjectEntity entity, List<ProjectMediaEntity> media) {
    ProjectPublicResponse res = toPublicResponse(entity);

    var picked = ProjectMediaPicker.pick(media, true);

    res.setCoverMediaUrl(picked.coverMediaUrl());
    res.setCoverMediaType(picked.coverMediaType());
    res.setBrochureUrl(picked.brochureUrl());
    res.setHasImages(picked.hasImages());
    res.setHasVideo(picked.hasVideo());

    return res;
  }
}
