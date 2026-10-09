package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.provider.enums.WorkerAvailabilityStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Partial update of public worker copy; null leaves a field unchanged. */
public record WorkerProfileUpdateRequest(
        @Size(max = 120) String displayName,
        @Size(max = 180) String headline,
        @Size(max = 2000) String bio,
        @Min(0) @Max(60) Integer experienceYears,
        WorkerAvailabilityStatus availability
) {
}
