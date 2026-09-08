package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dto.PublicCityDetailResponse;
import com.brandPitara.sfs.dto.TrendingCityCardResponse;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.mapper.CityMapper;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.service.PublicCityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicCityServiceImpl implements PublicCityService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final CityRepository cityRepository;

    @Override
    public List<TrendingCityCardResponse> getTrendingCities(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return cityRepository.findTrendingCityCards(
                ReviewStatus.APPROVED,
                PageRequest.of(0, safeLimit)
        );
    }

    @Override
    public PublicCityDetailResponse getCityBySlug(String citySlug) {
        CityEntity city = cityRepository.findBySlugIgnoreCaseAndActiveTrue(citySlug)
                .orElseThrow(() -> new NotFoundException("City not found: " + citySlug));
        return CityMapper.toPublicDetailResponse(city);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
