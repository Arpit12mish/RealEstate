package com.brandPitara.sfs.dashboard.city.service;

import com.brandPitara.sfs.dashboard.city.dto.DashboardCityUpsertRequest;
import com.brandPitara.sfs.dto.CityResponse;

import java.util.List;

public interface DashboardCityService {

    CityResponse create(DashboardCityUpsertRequest request);

    CityResponse update(Long cityId, DashboardCityUpsertRequest request);

    CityResponse updateCoverImage(Long cityId, String coverImageUrl);

    void delete(Long cityId);

    CityResponse get(Long cityId);

    List<CityResponse> list(String query);
}
