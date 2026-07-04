package com.brandPitara.sfs.project.mapper;

import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;
import com.brandPitara.sfs.project.repository.ProjectNearbyListingProjection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectNearbyListingMapperTest {

  @Test
  void toCardBuildsPublicNearbyCardFields() {
    ProjectNearbyListingCardDto card = ProjectNearbyListingMapper.toCard(row(1.234, 18_000_000L));

    assertThat(card.getProjectId()).isEqualTo(101L);
    assertThat(card.getProjectSlug()).isEqualTo("park-residences");
    assertThat(card.getCoverImageUrl()).isEqualTo("https://cdn.example/cover.jpg");
    assertThat(card.getBuilderId()).isEqualTo(2L);
    assertThat(card.getBuilderName()).isEqualTo("M3M");
    assertThat(card.getPriceLabel()).isEqualTo("₹1.8 Cr");
    assertThat(card.getUnitSummary()).isEqualTo("3 BHK · 1650 sqft");
    assertThat(card.getDistanceKm()).isEqualTo(1.23);
    assertThat(card.getDistanceLabel()).isEqualTo("1.2 km");
    assertThat(card.getFavoriteCount()).isZero();
    assertThat(card.getIsFavorite()).isFalse();
    assertThat(card.getVerified()).isTrue();
    assertThat(card.getAction().getType()).isEqualTo("NAVIGATE");
    assertThat(card.getAction().getTarget()).isEqualTo("PROJECT_DETAIL");
    assertThat(card.getAction().getPath()).isEqualTo("/projects/park-residences");
  }

  @Test
  void toCardOmitsDistanceLabelWhenDistanceIsNotCalculated() {
    ProjectNearbyListingCardDto card = ProjectNearbyListingMapper.toCard(row(null, 9_500_000L));

    assertThat(card.getDistanceKm()).isNull();
    assertThat(card.getDistanceLabel()).isNull();
    assertThat(card.getPriceLabel()).isEqualTo("₹95 L");
  }

  private static ProjectNearbyListingProjection row(Double distanceKm, Long priceMin) {
    return new ProjectNearbyListingProjection() {
      @Override public Long getProjectId() { return 101L; }
      @Override public String getProjectName() { return "Park Residences"; }
      @Override public String getProjectSlug() { return "park-residences"; }
      @Override public String getCoverImageUrl() { return "https://cdn.example/cover.jpg"; }
      @Override public String getAddressLine() { return "Sector 62"; }
      @Override public String getCityName() { return "Noida"; }
      @Override public Long getBuilderId() { return 2L; }
      @Override public String getBuilderName() { return "M3M"; }
      @Override public String getBuilderLogoUrl() { return "https://cdn.example/logo.png"; }
      @Override public Long getPriceMin() { return priceMin; }
      @Override public Long getPriceMax() { return null; }
      @Override public Double getLatitude() { return 28.627; }
      @Override public Double getLongitude() { return 77.375; }
      @Override public Double getDistanceKm() { return distanceKm; }
      @Override public Boolean getVerified() { return true; }
      @Override public String getUnitConfigurationType() { return "THREE_BHK"; }
      @Override public String getUnitLabel() { return null; }
      @Override public Integer getBedrooms() { return 3; }
      @Override public BigDecimal getSaleableAreaSqft() { return BigDecimal.valueOf(1650); }
      @Override public BigDecimal getSuperAreaSqft() { return null; }
      @Override public BigDecimal getCarpetAreaSqft() { return null; }
    };
  }
}
