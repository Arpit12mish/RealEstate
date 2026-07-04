package com.brandPitara.sfs.location.service.impl;

import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.location.dto.LocationResolveResponse;
import com.brandPitara.sfs.repository.CityRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LocationServiceImplTest {

  @Test
  void resolveCityContextUsesActiveDeviceCityWithoutProjectAvailabilityGate() {
    CityRepository cityRepository = mock(CityRepository.class);
    LocationServiceImpl service = new LocationServiceImpl(cityRepository);
    CityEntity delhi = city(1L, "Delhi", 28.6139, 77.2090);

    when(cityRepository.findFirstByNameIgnoreCaseAndActiveTrue("Delhi"))
        .thenReturn(Optional.of(delhi));

    LocationResolveResponse response = service.resolveCityContext(28.6139, 77.2090, "Delhi", 50.0);

    assertThat(response.isServiceable()).isTrue();
    assertThat(response.getCityId()).isEqualTo(1L);
    assertThat(response.getCityName()).isEqualTo("Delhi");
    assertThat(response.getMessage()).isNull();
  }

  @Test
  void resolveCityContextReturnsNeutralContextForUnknownDeviceCity() {
    CityRepository cityRepository = mock(CityRepository.class);
    LocationServiceImpl service = new LocationServiceImpl(cityRepository);

    when(cityRepository.findFirstByNameIgnoreCaseAndActiveTrue("Atlantis"))
        .thenReturn(Optional.empty());

    LocationResolveResponse response = service.resolveCityContext(28.6139, 77.2090, "Atlantis", null);

    assertThat(response.isServiceable()).isTrue();
    assertThat(response.getCityName()).isEqualTo("Atlantis");
    assertThat(response.getCityId()).isNull();
    assertThat(response.getMessage()).isNull();
  }

  @Test
  void resolveCityContextFindsNearestActiveCityWhenDeviceCityIsAbsent() {
    CityRepository cityRepository = mock(CityRepository.class);
    LocationServiceImpl service = new LocationServiceImpl(cityRepository);

    CityEntity delhi = city(1L, "Delhi", 28.6139, 77.2090);
    CityEntity noida = city(2L, "Noida", 28.5355, 77.3910);
    when(cityRepository.findAllWithCoordinates()).thenReturn(List.of(delhi, noida));

    LocationResolveResponse response = service.resolveCityContext(28.62, 77.21, null, null);

    assertThat(response.isServiceable()).isTrue();
    assertThat(response.getCityId()).isEqualTo(1L);
    assertThat(response.getCityName()).isEqualTo("Delhi");
    assertThat(response.getMessage()).isNull();
  }

  @Test
  void resolveCityContextDoesNotEmitUnsupportedCopyForKnownCityWithNoProjects() {
    CityRepository cityRepository = mock(CityRepository.class);
    LocationServiceImpl service = new LocationServiceImpl(cityRepository);
    CityEntity noida = city(2L, "Noida", 28.5355, 77.3910);

    when(cityRepository.findFirstByNameIgnoreCaseAndActiveTrue("Noida"))
        .thenReturn(Optional.of(noida));

    LocationResolveResponse response = service.resolveCityContext(null, null, "Noida", null);

    assertThat(response.isServiceable()).isTrue();
    assertThat(response.getCityId()).isEqualTo(2L);
    assertThat(response.getCityName()).isEqualTo("Noida");
    assertThat(response.getMessage()).isNull();
  }

  private static CityEntity city(Long id, String name, Double latitude, Double longitude) {
    return CityEntity.builder()
        .id(id)
        .name(name)
        .slug(name.toLowerCase())
        .state("Delhi NCR")
        .countryCode("IN")
        .latitude(latitude)
        .longitude(longitude)
        .active(true)
        .build();
  }
}
