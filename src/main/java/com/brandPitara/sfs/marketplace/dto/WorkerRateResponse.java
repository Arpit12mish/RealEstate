package com.brandPitara.sfs.marketplace.dto;

import com.brandPitara.sfs.provider.enums.ProviderRateType;
import com.brandPitara.sfs.provider.enums.ProviderRateUnit;

import java.math.BigDecimal;

/**
 * A typed worker charge. {@code label} is derived from {@code type} ("Visiting Charge",
 * "Service Fee", "Material Cost" ...), so the same amount is never shown under another name.
 */
public record WorkerRateResponse(
        ProviderRateType type,
        String label,
        BigDecimal amount,
        String currency,
        ProviderRateUnit unit,
        String note
) {
}
