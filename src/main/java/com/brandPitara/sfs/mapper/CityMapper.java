package com.brandPitara.sfs.mapper;

import com.brandPitara.sfs.dto.CityResponse;
import com.brandPitara.sfs.dto.PublicCityDetailResponse;
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

    /**
     * GAP-017. Deliberately narrower than {@link #toResponse}: no
     * {@code active}/{@code homepageFeatured} (admin/curation-oriented,
     * not part of the public city DTO convention — see
     * {@link com.brandPitara.sfs.dto.TrendingCityCardResponse}), no
     * {@code latitude}/{@code longitude}/{@code displayOrder} (not needed
     * by a standalone city detail page).
     */
    public static PublicCityDetailResponse toPublicDetailResponse(CityEntity e) {
        return PublicCityDetailResponse.builder()
                .id(e.getId())
                .slug(e.getSlug())
                .name(e.getName())
                .state(e.getState())
                .countryCode(e.getCountryCode())
                .coverImageUrl(e.getCoverImageUrl())
                .growthPercent(e.getGrowthPercent())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
