package com.brandPitara.sfs.marketplace.dto;

import com.brandPitara.sfs.provider.enums.WorkerAvailabilityStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

/** Public worker profile. Deliberately excludes userId, GST number and verification internals. */
@Builder
public record WorkerDetailResponse(
        Long id,
        String displayName,
        String initials,
        String avatarUrl,
        String trade,
        Long tradeCategoryId,
        String headline,
        String bio,
        /** "Serves Sector 14 & nearby" */
        String areaSummary,
        WorkerAvailabilityStatus availability,
        OffsetDateTime availabilityUpdatedAt,
        Integer experienceYears,
        List<WorkerRateResponse> rates,
        List<NamedItemResponse> services,
        List<ServiceAreaResponse> serviceAreas,
        PublicContactResponse contact
) {
    public record ServiceAreaResponse(Long cityId, String cityName, String locality, String label) {
    }
}
