package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectPublicV2Response;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;

public final class ProjectPublicV2Mapper {

    private ProjectPublicV2Mapper() {
    }

    public static ProjectPublicV2Response toResponse(ProjectPublicCoreData core) {
        return ProjectPublicV2Response.builder()
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
                .pricing(core.getPricing())
                .location(core.getLocation())
                .floorPlanGroups(core.getFloorPlanGroups())
                .connectivity(core.getConnectivity())
                .glimpses(core.getGlimpses())
                .amenities(core.getAmenities())
                .masterPlan(core.getMasterPlan())
                .build();
    }
}
