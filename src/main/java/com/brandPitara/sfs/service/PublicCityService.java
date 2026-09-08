package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.PublicCityDetailResponse;
import com.brandPitara.sfs.dto.TrendingCityCardResponse;

import java.util.List;

public interface PublicCityService {

    List<TrendingCityCardResponse> getTrendingCities(Integer limit);

    /**
     * GAP-017. Case-insensitive lookup; throws
     * {@link com.brandPitara.sfs.exception.NotFoundException} for an
     * unknown slug or an inactive city — the two are deliberately
     * indistinguishable to the caller (both surface as a plain 404),
     * matching every other entity's public detail-lookup convention in
     * this codebase.
     */
    PublicCityDetailResponse getCityBySlug(String citySlug);
}
