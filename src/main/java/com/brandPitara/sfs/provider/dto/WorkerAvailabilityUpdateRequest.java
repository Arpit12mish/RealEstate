package com.brandPitara.sfs.provider.dto;

import com.brandPitara.sfs.provider.enums.WorkerAvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record WorkerAvailabilityUpdateRequest(@NotNull WorkerAvailabilityStatus status) {
}
