package com.brandPitara.sfs.provider.dto;

import com.brandPitara.sfs.provider.enums.ProviderRateType;
import com.brandPitara.sfs.provider.enums.ProviderRateUnit;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WorkerRateRequest(
        @NotNull ProviderRateType type,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        /** ISO 4217, defaults to INR. */
        @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull ProviderRateUnit unit,
        @Size(max = 200) String note
) {
}
