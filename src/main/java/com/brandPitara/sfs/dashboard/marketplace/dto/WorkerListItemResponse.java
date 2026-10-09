package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.provider.enums.VerificationStatus;
import com.brandPitara.sfs.provider.enums.WorkerAvailabilityStatus;

public record WorkerListItemResponse(
        Long id,
        String displayName,
        String trade,
        VerificationStatus verificationStatus,
        WorkerAvailabilityStatus availability
) {
}
