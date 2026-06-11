package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnitConfigurationMetaResponse {
  private UnitConfigurationType value;
  private String label;
  private String group;
}
