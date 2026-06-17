package com.brandPitara.sfs.dashboard.city.service.impl;

import com.brandPitara.sfs.dashboard.city.dto.DashboardCityUpsertRequest;
import com.brandPitara.sfs.dto.CityResponse;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.repository.CityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardCityServiceImplTest {

    @Test
    void createGeneratesSlugAndPersistsHomepageMetadata() {
        CityRepository cityRepository = mock(CityRepository.class);
        DashboardCityServiceImpl service = new DashboardCityServiceImpl(cityRepository);

        DashboardCityUpsertRequest request = new DashboardCityUpsertRequest();
        request.setName("Greater Noida West");
        request.setState("Uttar Pradesh");
        request.setCoverImageUrl("https://cdn.sfs.com/cities/greater-noida-west.webp");
        request.setHomepageFeatured(true);
        request.setDisplayOrder(2);
        request.setGrowthPercent(11.5);

        when(cityRepository.findBySlugIgnoreCase("greater-noida-west")).thenReturn(Optional.empty());
        when(cityRepository.save(any(CityEntity.class))).thenAnswer(invocation -> {
            CityEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            return entity;
        });

        CityResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getSlug()).isEqualTo("greater-noida-west");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://cdn.sfs.com/cities/greater-noida-west.webp");
        assertThat(response.getHomepageFeatured()).isTrue();
        assertThat(response.getActive()).isTrue();
        assertThat(response.getDisplayOrder()).isEqualTo(2);
        assertThat(response.getGrowthPercent()).isEqualTo(11.5);
    }

    @Test
    void createRejectsDuplicateSlug() {
        CityRepository cityRepository = mock(CityRepository.class);
        DashboardCityServiceImpl service = new DashboardCityServiceImpl(cityRepository);

        DashboardCityUpsertRequest request = new DashboardCityUpsertRequest();
        request.setName("Mumbai");
        request.setSlug("mumbai");

        when(cityRepository.findBySlugIgnoreCase("mumbai"))
                .thenReturn(Optional.of(CityEntity.builder().id(7L).name("Mumbai").slug("mumbai").build()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("City slug already exists");

        verify(cityRepository, never()).save(any(CityEntity.class));
    }

    @Test
    void updateCoverImagePersistsOnlyCoverImageUrl() {
        CityRepository cityRepository = mock(CityRepository.class);
        DashboardCityServiceImpl service = new DashboardCityServiceImpl(cityRepository);
        CityEntity entity = CityEntity.builder()
                .id(7L)
                .name("Noida")
                .slug("noida")
                .state("Uttar Pradesh")
                .countryCode("IN")
                .coverImageUrl("https://cdn.sfs.com/old.webp")
                .active(true)
                .homepageFeatured(true)
                .displayOrder(3)
                .build();

        when(cityRepository.findById(7L)).thenReturn(Optional.of(entity));
        when(cityRepository.save(any(CityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CityResponse response = service.updateCoverImage(7L, " https://cdn.sfs.com/dashboard/cities/7/cover/new.webp ");

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getName()).isEqualTo("Noida");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://cdn.sfs.com/dashboard/cities/7/cover/new.webp");
        assertThat(entity.getCoverImageUrl()).isEqualTo("https://cdn.sfs.com/dashboard/cities/7/cover/new.webp");
    }
}
