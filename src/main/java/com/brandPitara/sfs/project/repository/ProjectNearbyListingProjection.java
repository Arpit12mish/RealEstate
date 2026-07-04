package com.brandPitara.sfs.project.repository;

import java.math.BigDecimal;

public interface ProjectNearbyListingProjection {
  Long getProjectId();
  String getProjectName();
  String getProjectSlug();
  String getCoverImageUrl();
  String getAddressLine();
  String getCityName();
  Long getBuilderId();
  String getBuilderName();
  String getBuilderLogoUrl();
  Long getPriceMin();
  Long getPriceMax();
  Double getLatitude();
  Double getLongitude();
  Double getDistanceKm();
  Boolean getVerified();
  String getUnitConfigurationType();
  String getUnitLabel();
  Integer getBedrooms();
  BigDecimal getSaleableAreaSqft();
  BigDecimal getSuperAreaSqft();
  BigDecimal getCarpetAreaSqft();
}
