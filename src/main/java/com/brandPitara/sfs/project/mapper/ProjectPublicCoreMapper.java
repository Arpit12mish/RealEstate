package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ProjectPublicCoreMapper {

    private ProjectPublicCoreMapper() {
    }

    public static ProjectPublicCoreData fromEntity(
            ProjectEntity entity,
            List<ProjectMediaEntity> media
    ) {
        List<ProjectMediaEntity> detachedMediaSource = media == null ? List.of() : List.copyOf(media);
        ProjectMediaPicker.Picked picked = ProjectMediaPicker.pick(detachedMediaSource, true);

        return ProjectPublicCoreData.builder()
                .id(entity.getId())
                .builderId(entity.getBuilder() != null ? entity.getBuilder().getId() : null)
                .builderName(entity.getBuilder() != null ? entity.getBuilder().getName() : null)
                .builderLogoUrl(entity.getBuilder() != null ? entity.getBuilder().getLogoUrl() : null)
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .cityId(entity.getCity() != null ? entity.getCity().getId() : null)
                .cityName(entity.getCity() != null ? entity.getCity().getName() : null)
                .addressLine(entity.getAddressLine())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .priceMin(entity.getPriceMin())
                .priceMax(entity.getPriceMax())
                .monthlyEmiMin(entity.getMonthlyEmiMin())
                .monthlyEmiMax(entity.getMonthlyEmiMax())
                .averagePricePerSqft(entity.getAveragePricePerSqft())
                .startDate(entity.getStartDate())
                .possessionDate(entity.getPossessionDate())
                .reraNumber(entity.getReraNumber())
                .status(entity.getStatus())
                .propertyTypes(copyPropertyTypes(entity))
                .coverMediaUrl(picked.coverMediaUrl())
                .coverMediaType(picked.coverMediaType())
                .brochureUrl(picked.brochureUrl())
                .hasImages(picked.hasImages())
                .hasVideo(picked.hasVideo())
                .glimpses(buildGlimpses(detachedMediaSource))
                .failedSections(Set.of())
                .build();
    }

    public static ProjectPublicResponse toV1Response(
            ProjectPublicCoreData core,
            boolean favorite
    ) {
        return ProjectPublicResponse.builder()
                .id(core.getId())
                .builderId(core.getBuilderId())
                .builderName(core.getBuilderName())
                .builderLogoUrl(core.getBuilderLogoUrl())
                .name(core.getName())
                .slug(core.getSlug())
                .description(core.getDescription())
                .cityId(core.getCityId())
                .cityName(core.getCityName())
                .addressLine(core.getAddressLine())
                .latitude(core.getLatitude())
                .longitude(core.getLongitude())
                .priceMin(core.getPriceMin())
                .priceMax(core.getPriceMax())
                .monthlyEmiMin(core.getMonthlyEmiMin())
                .monthlyEmiMax(core.getMonthlyEmiMax())
                .averagePricePerSqft(core.getAveragePricePerSqft())
                .startDate(core.getStartDate())
                .possessionDate(core.getPossessionDate())
                .reraNumber(core.getReraNumber())
                .status(core.getStatus())
                .propertyTypes(core.getPropertyTypes())
                .coverMediaUrl(core.getCoverMediaUrl())
                .coverMediaType(core.getCoverMediaType())
                .brochureUrl(core.getBrochureUrl())
                .hasImages(core.isHasImages())
                .hasVideo(core.isHasVideo())
                .favoriteCount(core.getFavoriteCount())
                .isFavorite(favorite)
                .pricing(core.getPricing())
                .location(core.getLocation())
                .floorPlanGroups(core.getFloorPlanGroups())
                .connectivity(core.getConnectivity())
                .glimpses(core.getGlimpses())
                .amenities(core.getAmenities())
                .masterPlan(core.getMasterPlan())
                .build();
    }

    private static Set<PropertyType> copyPropertyTypes(ProjectEntity project) {
        if (project.getPropertyTypes() == null || project.getPropertyTypes().isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(project.getPropertyTypes());
    }

    private static List<ProjectMediaResponse> buildGlimpses(List<ProjectMediaEntity> media) {
        return media.stream()
                .filter(Objects::nonNull)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .filter(item -> Boolean.FALSE.equals(item.getDeleted()))
                .filter(item -> item.getMediaType() == ProjectMediaType.IMAGE)
                .sorted(Comparator
                        .comparing(ProjectMediaEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProjectMediaEntity::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ProjectMediaMapper::toResponse)
                .toList();
    }
}
