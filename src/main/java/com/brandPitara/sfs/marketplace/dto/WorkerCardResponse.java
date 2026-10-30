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
        PublicContactResponse contact,
        /** One-line pitch, e.g. "Wall painting & waterproofing"; null when not entered. */
        String headline,
        /** First gallery photo (work), otherwise the profile photo; null when neither exists. */
        String coverImageUrl,
        /** "Serves Sector 14 & nearby"; null when no service area is set. */
        String areaSummary,
        /** "Available now" for workers reporting AVAILABLE; null otherwise. */
        String badge
) {
}
