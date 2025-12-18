package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.CityResponse;

import java.util.List;

public interface CityService {

    /**
     * Search cities by name (case-insensitive). If query is null/blank,
     * returns a default small list of cities.
     */
    List<CityResponse> searchCities(String query);
}
