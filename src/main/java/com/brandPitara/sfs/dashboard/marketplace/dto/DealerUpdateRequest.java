package com.brandPitara.sfs.dashboard.marketplace.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Partial update: null leaves a field unchanged, an empty string clears an optional text field.
 * Ratings and review counts are intentionally absent - they are derived from moderated reviews.
 */
public record DealerUpdateRequest(
        @Size(max = 255) String name,
        Long categoryId,
        Long cityId,
        @Size(max = 5000) String description,
        @Size(max = 20) String primaryPhone,
        @Size(max = 20) String whatsappPhone,
        @Email @Size(max = 255) String email,
        @Size(max = 255) String website,
        @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @Size(max = 255) String landmark,
        @Size(max = 120) String locality,
        @Size(max = 10) String pincode,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Min(1800) @Max(2200) Integer establishedYear,
        @Size(max = 64) String timezone,
        Boolean active
) {
}
