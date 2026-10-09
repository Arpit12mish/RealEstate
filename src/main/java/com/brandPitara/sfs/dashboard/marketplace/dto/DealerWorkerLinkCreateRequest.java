package com.brandPitara.sfs.dashboard.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Connects a worker to a dealer. Supplying a recommendationNote records the store's
 * recommendation as PENDING; it becomes public only after a reviewer VERIFIES it.
 */
public record DealerWorkerLinkCreateRequest(
        @NotNull Long workerId,
        @Size(max = 300) String recommendationNote,
        @Min(0) @Max(9999) Integer sortOrder
) {
}
