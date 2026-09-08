package com.brandPitara.sfs.projectmeter.mapper;

import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterProjectResponse;

import java.util.Set;

public final class ProjectMeterProjectMapper {

    private ProjectMeterProjectMapper() {
    }

    public static ProjectMeterProjectResponse toResponse(
        ProjectEntity project,
        String brochureUrl,
        boolean favorite,
        long favoriteCount
    ) {
        return ProjectMeterProjectResponse.builder()
            .id(project.getId())
            .name(project.getName())
            .slug(project.getSlug())
            .description(project.getDescription())
            .builderId(project.getBuilder() != null ? project.getBuilder().getId() : null)
            .builderName(project.getBuilder() != null ? project.getBuilder().getName() : null)
            .builderLogoUrl(project.getBuilder() != null ? project.getBuilder().getLogoUrl() : null)
            .cityId(project.getCity() != null ? project.getCity().getId() : null)
            .cityName(project.getCity() != null ? project.getCity().getName() : null)
            .addressLine(project.getAddressLine())
            .latitude(project.getLatitude())
            .longitude(project.getLongitude())
            .priceMin(project.getPriceMin())
            .priceMax(project.getPriceMax())
            .monthlyEmiMin(project.getMonthlyEmiMin())
            .monthlyEmiMax(project.getMonthlyEmiMax())
            .averagePricePerSqft(project.getAveragePricePerSqft())
            .startDate(project.getStartDate())
            .possessionDate(project.getPossessionDate())
            .reraNumber(project.getReraNumber())
            .status(project.getStatus())
            .propertyTypes(project.getPropertyTypes() == null || project.getPropertyTypes().isEmpty()
                ? Set.of()
                : Set.copyOf(project.getPropertyTypes()))
            .brochureUrl(brochureUrl)
            .isFavorite(favorite)
            .favoriteCount(favoriteCount)
            .build();
    }
}
