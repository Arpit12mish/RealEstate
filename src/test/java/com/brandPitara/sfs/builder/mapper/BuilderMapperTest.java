package com.brandPitara.sfs.builder.mapper;

import com.brandPitara.sfs.builder.dto.BuilderCardResponse;
import com.brandPitara.sfs.builder.dto.BuilderPublicResponse;
import com.brandPitara.sfs.builder.dto.BuilderResponse;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.entity.CityEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuilderMapperTest {

  private BuilderEntity.BuilderEntityBuilder baseBuilder() {
    return BuilderEntity.builder()
        .id(701L)
        .name("Meridian Constructions")
        .slug("meridian-constructions")
        .logoUrl("logo.png")
        .description("A builder")
        .addressLine("12th Floor, MG Road")
        .latitude(12.9716)
        .longitude(77.5946)
        .active(true)
        .published(true)
        .priority(0)
        .deleted(false);
  }

  @Test
  void toPublicResponse_includesSlug() {
    BuilderPublicResponse response = BuilderMapper.toPublicResponse(baseBuilder().build());

    assertThat(response.getId()).isEqualTo(701L);
    assertThat(response.getSlug()).isEqualTo("meridian-constructions");
    assertThat(response.getName()).isEqualTo("Meridian Constructions");
  }

  @Test
  void toPublicResponse_resolvesCityIdAndNameFromCityEntity_whenCityPresent() {
    CityEntity city = CityEntity.builder().id(21L).name("Bengaluru").build();
    BuilderPublicResponse response = BuilderMapper.toPublicResponse(baseBuilder().city(city).build());

    assertThat(response.getCityId()).isEqualTo(21L);
    assertThat(response.getCityName()).isEqualTo("Bengaluru");
  }

  @Test
  void toPublicResponse_cityIdAndNameAreNull_whenNoCityAssigned() {
    BuilderPublicResponse response = BuilderMapper.toPublicResponse(baseBuilder().city(null).build());

    assertThat(response.getCityId()).isNull();
    assertThat(response.getCityName()).isNull();
  }

  @Test
  void toResponse_isUnaffectedByTheNewSlugField_noSlugGetterExposed() {
    BuilderResponse response = BuilderMapper.toResponse(baseBuilder().build());

    assertThat(response.getId()).isEqualTo(701L);
    assertThat(response.getName()).isEqualTo("Meridian Constructions");
    // BuilderResponse (admin/dashboard DTO) is out of this phase's scope - it never
    // gained a slug getter, confirming toResponse() genuinely wasn't touched.
    assertThat(BuilderResponse.class.getDeclaredMethods())
        .noneMatch(m -> m.getName().equals("getSlug"));
  }

  @Test
  void toCard_isUnaffectedByTheNewSlugField_noSlugGetterExposed() {
    BuilderCardResponse response = BuilderMapper.toCard(baseBuilder().build());

    assertThat(response.getId()).isEqualTo(701L);
    assertThat(response.getName()).isEqualTo("Meridian Constructions");
    assertThat(BuilderCardResponse.class.getDeclaredMethods())
        .noneMatch(m -> m.getName().equals("getSlug"));
  }
}
