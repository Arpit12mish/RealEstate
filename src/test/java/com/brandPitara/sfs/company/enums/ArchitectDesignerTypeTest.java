package com.brandPitara.sfs.company.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectDesignerTypeTest {

  @Test
  void architectStorageValuesMatchTheVerifiedHomeSectionSet() {
    assertThat(ArchitectDesignerType.ARCHITECT.storageValues())
        .containsExactlyInAnyOrder("ARCHITECT", "ARCHITECT&DESIGNERS");
  }

  @Test
  void interiorDesignerStorageValuesMatchTheVerifiedHomeSectionSet() {
    assertThat(ArchitectDesignerType.INTERIOR_DESIGNER.storageValues())
        .containsExactlyInAnyOrder("DESIGNER", "DESIGNERS", "INTERIOR_DESIGNER");
  }

  @Test
  void architectAndInteriorDesignerStorageValuesNeverOverlap() {
    assertThat(ArchitectDesignerType.ARCHITECT.storageValues())
        .doesNotContainAnyElementsOf(ArchitectDesignerType.INTERIOR_DESIGNER.storageValues());
  }

  @ParameterizedTest
  @CsvSource({
      "ARCHITECT, ARCHITECT",
      "architect, ARCHITECT",
      "Architect, ARCHITECT",
      " ARCHITECT , ARCHITECT",
      "INTERIOR_DESIGNER, INTERIOR_DESIGNER",
      "interior_designer, INTERIOR_DESIGNER",
  })
  void parseIsCaseInsensitiveAndTrims(String raw, ArchitectDesignerType expected) {
    assertThat(ArchitectDesignerType.parse(raw)).contains(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "DESIGNER", "DESIGNERS", "ARCHITECT&DESIGNERS", // storage values, not the public normalized name
      "ARCHITECTS", "not-a-type", "INTERIOR-DESIGNER", // unknown or wrongly-punctuated strings
  })
  void parseRejectsStorageValuesAndUnknownStrings(String raw) {
    assertThat(ArchitectDesignerType.parse(raw)).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void parseRejectsNullAndBlank(String raw) {
    assertThat(ArchitectDesignerType.parse(raw)).isEmpty();
  }
}
