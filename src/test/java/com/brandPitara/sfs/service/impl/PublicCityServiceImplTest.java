package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dto.PublicCityDetailResponse;
import com.brandPitara.sfs.dto.TrendingCityCardResponse;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.repository.CityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicCityServiceImplTest {

    @Mock private CityRepository cityRepository;

    @InjectMocks private PublicCityServiceImpl service;

    @Test
    void getTrendingCitiesUsesApprovedProjectCountsAndDefaultLimit() {
        when(cityRepository.findTrendingCityCards(
                org.mockito.ArgumentMatchers.eq(ReviewStatus.APPROVED),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(List.of(cityCard()));

        List<TrendingCityCardResponse> response = service.getTrendingCities(null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cityRepository).findTrendingCityCards(
                org.mockito.ArgumentMatchers.eq(ReviewStatus.APPROVED),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getProjectCount()).isEqualTo(8420L);
    }

    @Test
    void getTrendingCitiesCapsLimitAt20() {
        when(cityRepository.findTrendingCityCards(
                org.mockito.ArgumentMatchers.eq(ReviewStatus.APPROVED),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(List.of());

        service.getTrendingCities(999);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cityRepository).findTrendingCityCards(
                org.mockito.ArgumentMatchers.eq(ReviewStatus.APPROVED),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getCityBySlugReturnsMappedDetailForAKnownActiveCity() {
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        when(cityRepository.findBySlugIgnoreCaseAndActiveTrue(eq("mumbai")))
                .thenReturn(Optional.of(cityEntity(updatedAt)));

        PublicCityDetailResponse response = service.getCityBySlug("mumbai");

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getSlug()).isEqualTo("mumbai");
        assertThat(response.getName()).isEqualTo("Mumbai");
        assertThat(response.getState()).isEqualTo("Maharashtra");
        assertThat(response.getCountryCode()).isEqualTo("IN");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://cdn.sfs.com/cities/mumbai.webp");
        assertThat(response.getGrowthPercent()).isEqualTo(12.4);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void getCityBySlugThrowsNotFoundForAnUnknownSlug() {
        when(cityRepository.findBySlugIgnoreCaseAndActiveTrue(eq("nowhere-city")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCityBySlug("nowhere-city"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCityBySlugThrowsNotFoundForAnInactiveCity() {
        // findBySlugIgnoreCaseAndActiveTrue already excludes inactive rows at the
        // query level, so from this service's perspective an inactive city is
        // indistinguishable from an unknown one - both return Optional.empty(),
        // which is the deliberate, tested behavior (see PublicCityService's own
        // javadoc: unknown vs. inactive must not be a distinguishable signal).
        when(cityRepository.findBySlugIgnoreCaseAndActiveTrue(eq("retired-town")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCityBySlug("retired-town"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getCityBySlugDelegatesTheExactRequestedSlugToTheCaseInsensitiveRepositoryMethod() {
        // Case-folding is the repository method's own job (IgnoreCase) - the
        // service must not pre-lowercase/mutate the slug itself.
        when(cityRepository.findBySlugIgnoreCaseAndActiveTrue(eq("MUMBAI")))
                .thenReturn(Optional.of(cityEntity(OffsetDateTime.now())));

        service.getCityBySlug("MUMBAI");

        verify(cityRepository).findBySlugIgnoreCaseAndActiveTrue("MUMBAI");
    }

    private CityEntity cityEntity(OffsetDateTime updatedAt) {
        CityEntity entity = CityEntity.builder()
                .id(7L)
                .name("Mumbai")
                .slug("mumbai")
                .state("Maharashtra")
                .countryCode("IN")
                .coverImageUrl("https://cdn.sfs.com/cities/mumbai.webp")
                .active(true)
                .homepageFeatured(true)
                .displayOrder(1)
                .growthPercent(12.4)
                .build();
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    private TrendingCityCardResponse cityCard() {
        return TrendingCityCardResponse.builder()
                .id(7L)
                .name("Mumbai")
                .slug("mumbai")
                .state("Maharashtra")
                .countryCode("IN")
                .coverImageUrl("https://cdn.sfs.com/cities/mumbai.webp")
                .projectCount(8420L)
                .growthPercent(12.4)
                .displayOrder(1)
                .comingSoon(false)
                .build();
    }
}
