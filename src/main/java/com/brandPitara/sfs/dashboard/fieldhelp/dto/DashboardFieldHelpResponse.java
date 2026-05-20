package com.brandPitara.sfs.dashboard.fieldhelp.dto;

import com.brandPitara.sfs.dashboard.common.enums.DashboardHelpModule;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardFieldHelpResponse {
    Long id;
    DashboardHelpModule module;
    String fieldKey;
    String fieldLabel;
    String shortHelp;
    String detailedHelp;
    String whyNeeded;
    String sourceHint;
    String exampleValue;
    String validationHint;
    Boolean active;
    Integer displayOrder;
}
