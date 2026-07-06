package com.brandPitara.sfs.brand.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrandCompanyClassifierTest {

  @Test
  void classifiesPlainArchitect() {
    assertThat(BrandCompanyClassifier.isArchitect("ARCHITECT")).isTrue();
    assertThat(BrandCompanyClassifier.isDesigner("ARCHITECT")).isFalse();
  }

  @Test
  void classifiesInteriorDesignerViaDesignerOrInteriorKeyword() {
    assertThat(BrandCompanyClassifier.isDesigner("INTERIOR_DESIGNER")).isTrue();
    assertThat(BrandCompanyClassifier.isArchitect("INTERIOR_DESIGNER")).isFalse();

    assertThat(BrandCompanyClassifier.isDesigner("DESIGNER")).isTrue();
    assertThat(BrandCompanyClassifier.isDesigner("INTERIOR")).isTrue();
  }

  @Test
  void combinedCompanyTypeCountsAsBothIndependently() {
    // Real, messy data seen in this codebase (no enum/CHECK constraint on company_type).
    assertThat(BrandCompanyClassifier.isArchitect("ARCHITECT&DESIGNERS")).isTrue();
    assertThat(BrandCompanyClassifier.isDesigner("ARCHITECT&DESIGNERS")).isTrue();
  }

  @Test
  void isCaseInsensitive() {
    assertThat(BrandCompanyClassifier.isArchitect("architect")).isTrue();
    assertThat(BrandCompanyClassifier.isDesigner("interior designer")).isTrue();
  }

  @Test
  void returnsFalse_forNullOrBlankOrUnrelatedType() {
    assertThat(BrandCompanyClassifier.isArchitect(null)).isFalse();
    assertThat(BrandCompanyClassifier.isDesigner(null)).isFalse();
    assertThat(BrandCompanyClassifier.isArchitect("")).isFalse();
    assertThat(BrandCompanyClassifier.isArchitect("BUILDER")).isFalse();
    assertThat(BrandCompanyClassifier.isDesigner("BUILDER")).isFalse();
  }
}
