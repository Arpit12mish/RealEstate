package com.brandPitara.sfs.dashboard.city.service.impl;

import com.brandPitara.sfs.dashboard.city.dto.DashboardCityUpsertRequest;
import com.brandPitara.sfs.dashboard.city.service.DashboardCityService;
import com.brandPitara.sfs.dto.CityResponse;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardCityServiceImpl implements DashboardCityService {

    private final CityRepository cityRepository;

    @Override
    @Transactional
    public CityResponse create(DashboardCityUpsertRequest request) {
        CityEntity entity = CityEntity.builder()
                .name(request.getName().trim())
                .state(clean(request.getState()))
                .countryCode(StringUtils.hasText(request.getCountryCode()) ? request.getCountryCode().trim() : "IN")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        return toResponse(cityRepository.save(entity));
    }

    @Override
    @Transactional
    public CityResponse update(Long cityId, DashboardCityUpsertRequest request) {
        CityEntity entity = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found: " + cityId));

        entity.setName(request.getName().trim());
        if (request.getState() != null)       entity.setState(clean(request.getState()));
        if (request.getCountryCode() != null) entity.setCountryCode(request.getCountryCode().trim());
        if (request.getLatitude() != null)    entity.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)   entity.setLongitude(request.getLongitude());

        return toResponse(cityRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long cityId) {
        if (!cityRepository.existsById(cityId)) {
            throw new EntityNotFoundException("City not found: " + cityId);
        }
        cityRepository.deleteById(cityId);
    }

    @Override
    @Transactional(readOnly = true)
    public CityResponse get(Long cityId) {
        return cityRepository.findById(cityId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("City not found: " + cityId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityResponse> list(String query) {
        List<CityEntity> entities = StringUtils.hasText(query)
                ? cityRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query)
                : cityRepository.findTop50ByOrderByNameAsc();
        return entities.stream().map(this::toResponse).toList();
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

    private String clean(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
