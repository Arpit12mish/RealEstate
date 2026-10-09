package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.BusinessMediaUsage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record DealerMediaUpdateRequest(
        BusinessMediaUsage usageType,
        @Size(max = 255) String altText,
        @Min(0) @Max(9999) Integer sortOrder,
        Boolean active
) {
}
