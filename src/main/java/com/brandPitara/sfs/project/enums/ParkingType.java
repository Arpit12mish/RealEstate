package com.brandPitara.sfs.project.enums;

public enum ParkingType {
  OPEN("Open"),
  COVERED("Covered"),
  BASEMENT("Basement"),
  STILT("Stilt"),
  MECHANICAL("Mechanical"),
  MIXED("Mixed"),
  NOT_DISCLOSED("Not disclosed");

  private final String displayLabel;

  ParkingType(String displayLabel) {
    this.displayLabel = displayLabel;
  }

  public String displayLabel() {
    return displayLabel;
  }
}
