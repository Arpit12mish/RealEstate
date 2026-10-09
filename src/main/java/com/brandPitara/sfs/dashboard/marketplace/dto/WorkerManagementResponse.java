package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.marketplace.dto.NamedItemResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerRateResponse;
import com.brandPitara.sfs.provider.enums.VerificationStatus;
import com.brandPitara.sfs.provider.enums.WorkerAvailabilityStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record WorkerManagementResponse(
        Long id,
        String displayName,
        String trade,
        String headline,
        String bio,
        Integer experienceYears,
        VerificationStatus verificationStatus,
        WorkerAvailabilityStatus availability,
        OffsetDateTime availabilityUpdatedAt,
        Long listingBusinessId,
        String listingPhone,
        String listingWhatsapp,
        boolean listingActive,
        List<String> serviceAreas,
        List<NamedItemResponse> services,
        List<WorkerRateResponse> rates
) {
}
