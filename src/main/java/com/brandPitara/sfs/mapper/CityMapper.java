package com.brandPitara.sfs.mapper;

import com.brandPitara.sfs.dto.CityResponse;
import com.brandPitara.sfs.entity.CityEntity;

public final class CityMapper {

    private CityMapper() {
    }

    public static CityResponse toResponse(CityEntity e) {
        return CityResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .slug(e.getSlug())
                .state(e.getState())
                .countryCode(e.getCountryCode())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .coverImageUrl(e.getCoverImageUrl())
                .active(e.getActive())
                .homepageFeatured(e.getHomepageFeatured())
                .displayOrder(e.getDisplayOrder())
                .growthPercent(e.getGrowthPercent())
                .build();
    }
}
