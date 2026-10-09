package com.brandPitara.sfs.marketplace.dto;

import com.brandPitara.sfs.provider.enums.WorkerAvailabilityStatus;
import lombok.Builder;

/** Compact worker projection used by "Connected Workers". */
@Builder
public record WorkerCardResponse(
        Long id,
        String displayName,
        String initials,
        String avatarUrl,
        String trade,
        WorkerAvailabilityStatus availability,
        /** Visiting charge when present, otherwise the worker's first listed rate. */
        WorkerRateResponse primaryRate,
        Integer experienceYears,
        PublicContactResponse contact
) {
}
