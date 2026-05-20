package com.brandPitara.sfs.dashboard.fieldhelp.dto;

import com.brandPitara.sfs.dashboard.common.enums.DashboardHelpModule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DashboardFieldHelpUpsertRequest {

    @NotNull
    private DashboardHelpModule module;

    @NotBlank
    private String fieldKey;

    @NotBlank
    private String fieldLabel;

    @NotBlank
    private String shortHelp;

    private String detailedHelp;
    private String whyNeeded;
    private String sourceHint;
    private String exampleValue;
    private String validationHint;
    private Boolean active;
    private Integer displayOrder;
}
