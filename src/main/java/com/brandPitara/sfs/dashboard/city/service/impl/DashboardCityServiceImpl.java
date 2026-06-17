package com.brandPitara.sfs.dashboard.city.service.impl;

import com.brandPitara.sfs.dashboard.city.dto.DashboardCityUpsertRequest;
import com.brandPitara.sfs.dashboard.city.service.DashboardCityService;
import com.brandPitara.sfs.dto.CityResponse;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.mapper.CityMapper;
import com.brandPitara.sfs.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DashboardCityServiceImpl implements DashboardCityService {

    private final CityRepository cityRepository;

    @Override
    @Transactional
    public CityResponse create(DashboardCityUpsertRequest request) {
        String slug = normalizeSlug(firstNonBlank(request.getSlug(), request.getName()));
        validateSlugAvailableForCreate(slug);

        CityEntity entity = CityEntity.builder()
                .name(request.getName().trim())
                .slug(slug)
                .state(clean(request.getState()))
                .countryCode(StringUtils.hasText(request.getCountryCode()) ? request.getCountryCode().trim() : "IN")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .coverImageUrl(clean(request.getCoverImageUrl()))
                .active(request.getActive() != null ? request.getActive() : true)
                .homepageFeatured(request.getHomepageFeatured() != null ? request.getHomepageFeatured() : false)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .growthPercent(request.getGrowthPercent())
                .build();
        return CityMapper.toResponse(cityRepository.save(entity));
    }

    @Override
    @Transactional
    public CityResponse update(Long cityId, DashboardCityUpsertRequest request) {
        CityEntity entity = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found: " + cityId));

        entity.setName(request.getName().trim());
        if (request.getSlug() != null || !StringUtils.hasText(entity.getSlug())) {
            String slug = normalizeSlug(firstNonBlank(request.getSlug(), request.getName()));
            if (!Objects.equals(slug, entity.getSlug())) {
                validateSlugAvailableForUpdate(slug, cityId);
            }
            entity.setSlug(slug);
        }
        if (request.getState() != null)       entity.setState(clean(request.getState()));
        if (request.getCountryCode() != null) entity.setCountryCode(request.getCountryCode().trim());
        if (request.getLatitude() != null)    entity.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)   entity.setLongitude(request.getLongitude());
        if (request.getCoverImageUrl() != null) entity.setCoverImageUrl(clean(request.getCoverImageUrl()));
        if (request.getActive() != null) entity.setActive(request.getActive());
        if (request.getHomepageFeatured() != null) entity.setHomepageFeatured(request.getHomepageFeatured());
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());
        if (request.getGrowthPercent() != null) entity.setGrowthPercent(request.getGrowthPercent());

        return CityMapper.toResponse(cityRepository.save(entity));
    }

    @Override
    @Transactional
    public CityResponse updateCoverImage(Long cityId, String coverImageUrl) {
        CityEntity entity = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found: " + cityId));

        entity.setCoverImageUrl(clean(coverImageUrl));
        return CityMapper.toResponse(cityRepository.save(entity));
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
                .map(CityMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("City not found: " + cityId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityResponse> list(String query) {
        List<CityEntity> entities = StringUtils.hasText(query)
                ? cityRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query)
                : cityRepository.findTop50ByOrderByNameAsc();
        return entities.stream().map(CityMapper::toResponse).toList();
    }

    private String clean(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private String normalizeSlug(String value) {
        String source = StringUtils.hasText(value) ? value.trim() : "city";
        String slug = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return StringUtils.hasText(slug) ? slug : "city";
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private void validateSlugAvailableForCreate(String slug) {
        cityRepository.findBySlugIgnoreCase(slug).ifPresent(existing -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "City slug already exists: " + slug + " cityId=" + existing.getId()
            );
        });
    }

    private void validateSlugAvailableForUpdate(String slug, Long cityId) {
        cityRepository.findBySlugIgnoreCaseAndIdNot(slug, cityId).ifPresent(existing -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "City slug already exists: " + slug + " cityId=" + existing.getId()
            );
        });
    }
}
