package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.CityResponse;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;

    @Override
    public List<CityResponse> searchCities(String query) {
        List<CityEntity> cities;

        if (query == null || query.isBlank()) {
            cities = cityRepository.findTop50ByOrderByNameAsc();
        } else {
            cities = cityRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.trim());
        }

        return cities.stream()
                .map(this::toResponse)
                .toList();
    }

    private CityResponse toResponse(CityEntity e) {
        return CityResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .state(e.getState())
                .countryCode(e.getCountryCode())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .build();
    }
}
