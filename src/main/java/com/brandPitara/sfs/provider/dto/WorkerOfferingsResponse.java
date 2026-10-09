package com.brandPitara.sfs.provider.dto;

import com.brandPitara.sfs.marketplace.dto.NamedItemResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerRateResponse;
import com.brandPitara.sfs.provider.enums.WorkerAvailabilityStatus;

import java.time.OffsetDateTime;
import java.util.List;

/** The worker-editable marketplace fields, returned after any self-service or dashboard change. */
public record WorkerOfferingsResponse(
        Long workerId,
        WorkerAvailabilityStatus availability,
        OffsetDateTime availabilityUpdatedAt,
        List<NamedItemResponse> services,
        List<WorkerRateResponse> rates
) {
}
