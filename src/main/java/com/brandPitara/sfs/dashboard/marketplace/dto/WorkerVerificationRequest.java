package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.provider.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;

/** Only VERIFIED workers are publicly listed. */
public record WorkerVerificationRequest(@NotNull VerificationStatus status) {
}
