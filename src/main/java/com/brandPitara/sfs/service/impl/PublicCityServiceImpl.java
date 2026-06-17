package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dto.TrendingCityCardResponse;
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

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
