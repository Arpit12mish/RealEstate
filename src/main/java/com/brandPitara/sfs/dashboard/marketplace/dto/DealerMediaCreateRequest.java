package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.BusinessMediaUsage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registers an image uploaded via POST /api/dashboard/media/presign (uploadType DEALER_MEDIA_IMAGE). */
public record DealerMediaCreateRequest(
        @NotNull BusinessMediaUsage usageType,
        @NotBlank @Pattern(regexp = "^https://\\S+$", message = "mediaUrl must be an https URL") String mediaUrl,
        @Size(max = 500) String storageKey,
        @Size(max = 255) String altText,
        @Min(0) @Max(9999) Integer sortOrder
) {
}
