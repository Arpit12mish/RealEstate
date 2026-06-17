package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dto.TrendingCityCardResponse;
import com.brandPitara.sfs.repository.CityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
