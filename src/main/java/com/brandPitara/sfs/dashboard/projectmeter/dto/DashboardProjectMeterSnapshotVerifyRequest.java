package com.brandPitara.sfs.dashboard.projectmeter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DashboardProjectMeterSnapshotVerifyRequest {

    @NotNull(message = "verified must not be null")
    private Boolean verified;
}
