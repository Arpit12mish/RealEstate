package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.CityResponse;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.mapper.CityMapper;
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
                .map(CityMapper::toResponse)
                .toList();
    }
}
