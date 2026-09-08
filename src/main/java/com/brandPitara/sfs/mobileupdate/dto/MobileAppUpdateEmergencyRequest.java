package com.brandPitara.sfs.mobileupdate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MobileAppUpdateEmergencyRequest(
        @NotNull Boolean disabled,
        @NotBlank @Size(max = 500) String changeReason,
        @NotNull @PositiveOrZero Long expectedVersion
) {
}

