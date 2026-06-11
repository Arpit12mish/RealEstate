package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectConnectivityCategoryResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityPlaceResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.entity.ProjectConnectivityEntity;
import com.brandPitara.sfs.project.entity.ProjectConnectivityPlaceEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectConnectivityMapper {

  private static final List<ProjectConnectivityCategory> PUBLIC_CATEGORY_ORDER = List.of(
      ProjectConnectivityCategory.TRANSIT,
      ProjectConnectivityCategory.SCHOOLS,
      ProjectConnectivityCategory.COLLEGES,
      ProjectConnectivityCategory.HOSPITALS,
      ProjectConnectivityCategory.PARKS,
      ProjectConnectivityCategory.RETAIL,
      ProjectConnectivityCategory.MALLS,
      ProjectConnectivityCategory.GYMS,
      ProjectConnectivityCategory.OFFICES,
      ProjectConnectivityCategory.RESTAURANTS,
      ProjectConnectivityCategory.BANKS,
      ProjectConnectivityCategory.DAILY_NEEDS,
      ProjectConnectivityCategory.SAFETY,
      ProjectConnectivityCategory.LIFESTYLE
  );

  public static ProjectConnectivityPlaceResponse toPlaceResponse(ProjectConnectivityPlaceEntity e) {
    ProjectConnectivityCategory category = e.getCategory() != null
        ? e.getCategory()
        : ProjectConnectivityCategoryMapper.fromType(e.getPlaceType());

    return ProjectConnectivityPlaceResponse.builder()
        .id(e.getId())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .placeName(e.getPlaceName())
        .placeType(e.getPlaceType())
        .placeTypeLabel(e.getPlaceType() != null ? e.getPlaceType().toLabel() : null)
        .category(category)
        .categoryLabel(category != null ? category.toLabel() : null)
        .distanceLabel(e.getDistanceLabel())
        .distanceMeters(e.getDistanceMeters())
        .durationLabel(e.getDurationLabel())
        .durationSeconds(e.getDurationSeconds())
        .latitude(e.getLatitude())
        .longitude(e.getLongitude())
        .imageUrl(e.getImageUrl())
        .address(e.getAddress())
        .externalPlaceId(e.getExternalPlaceId())
        .provider(e.getProvider())
        .rating(e.getRating())
        .userRatingCount(e.getUserRatingCount())
        .sortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .verified(Boolean.TRUE.equals(e.getVerified()))
        .featured(Boolean.TRUE.equals(e.getFeatured()))
        .build();
  }

  public static ProjectConnectivityResponse toResponse(
      ProjectConnectivityEntity connectivity,
      ProjectEntity project,
      List<ProjectConnectivityPlaceEntity> places
  ) {
    List<ProjectConnectivityPlaceResponse> placeResponses = places == null
        ? List.of()
        : places.stream()
            .map(ProjectConnectivityMapper::toPlaceResponse)
            .sorted(placeResponseComparator())
            .toList();

    return ProjectConnectivityResponse.builder()
        .projectId(project != null ? project.getId() : null)
        .title(connectivity != null ? connectivity.getTitle() : null)
        .subtitle(connectivity != null ? connectivity.getSubtitle() : null)
        .summary(connectivity != null ? connectivity.getSummary() : null)
        .mapImageUrl(connectivity != null ? connectivity.getMapImageUrl() : null)
        .projectLatitude(project != null ? project.getLatitude() : null)
        .projectLongitude(project != null ? project.getLongitude() : null)
        .projectAddress(project != null ? project.getAddressLine() : null)
        .defaultRadiusMeters(connectivity != null ? connectivity.getDefaultRadiusMeters() : null)
        .searchEnabled(connectivity != null && Boolean.TRUE.equals(connectivity.getSearchEnabled()))
        .places(placeResponses)
        .categories(buildCategories(placeResponses))
        .build();
  }

  private static List<ProjectConnectivityCategoryResponse> buildCategories(
      List<ProjectConnectivityPlaceResponse> places
  ) {
    Map<ProjectConnectivityCategory, List<ProjectConnectivityPlaceResponse>> grouped =
        places.stream()
            .filter(p -> p.getCategory() != null)
            .filter(p -> p.getCategory() != ProjectConnectivityCategory.SEARCH)
            .collect(Collectors.groupingBy(ProjectConnectivityPlaceResponse::getCategory));

    return PUBLIC_CATEGORY_ORDER.stream()
        .map(category -> {
          List<ProjectConnectivityPlaceResponse> items = grouped.getOrDefault(category, List.of())
              .stream()
              .sorted(placeResponseComparator())
              .toList();

          return ProjectConnectivityCategoryResponse.builder()
              .category(category)
              .categoryLabel(category.toLabel())
              .iconKey(category.iconKey())
              .count(items.size())
              .places(items)
              .build();
        })
        .filter(group -> group.getCount() != null && group.getCount() > 0)
        .toList();
  }

  private static Comparator<ProjectConnectivityPlaceResponse> placeResponseComparator() {
    return Comparator
        .comparing(ProjectConnectivityPlaceResponse::isFeatured).reversed()
        .thenComparing(ProjectConnectivityPlaceResponse::getSortOrder)
        .thenComparing(
            ProjectConnectivityPlaceResponse::getDistanceMeters,
            Comparator.nullsLast(Integer::compareTo)
        )
        .thenComparing(
            ProjectConnectivityPlaceResponse::getId,
            Comparator.nullsLast(Long::compareTo)
        );
  }
}
