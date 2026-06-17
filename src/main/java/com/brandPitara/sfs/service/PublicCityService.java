package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.TrendingCityCardResponse;

import java.util.List;

public interface PublicCityService {

    List<TrendingCityCardResponse> getTrendingCities(Integer limit);
}
