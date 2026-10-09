package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.WorkerLinkStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Editing the recommendation note re-queues it as PENDING so the public text is always reviewed;
 * an empty note withdraws the recommendation (NONE).
 */
public record DealerWorkerLinkUpdateRequest(
        WorkerLinkStatus status,
        @Size(max = 300) String recommendationNote,
        @Min(0) @Max(9999) Integer sortOrder
) {
}
