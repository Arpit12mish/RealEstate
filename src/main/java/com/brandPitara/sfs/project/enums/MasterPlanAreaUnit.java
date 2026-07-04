package com.brandPitara.sfs.project.enums;

public enum MasterPlanAreaUnit {
  SQ_FT("sq ft"),
  SQ_MT("sq m"),
  ACRE("Acres"),
  HECTARE("Hectares");

  private final String displayLabel;

  MasterPlanAreaUnit(String displayLabel) {
    this.displayLabel = displayLabel;
  }

  public String displayLabel() {
    return displayLabel;
  }
}
