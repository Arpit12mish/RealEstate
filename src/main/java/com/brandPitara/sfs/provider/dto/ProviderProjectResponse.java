package com.brandPitara.sfs.provider.dto;

import com.brandPitara.sfs.provider.enums.ProjectVisibility;

import java.util.List;

public record ProviderProjectResponse(
    Long id,
    Long providerId,
    String title,
    String description,
    Long categoryId,
    String categoryName,
    Long cityId,
    String cityName,
    String locality,
    Integer budgetMin,
    Integer budgetMax,
    ProjectVisibility visibility,
    List<ProjectMediaResponse> media
) {
  public record ProjectMediaResponse(
      Long id,
      String mediaType,
      String url,
      String thumbnailUrl,
      int sortOrder
  ) {}
}
