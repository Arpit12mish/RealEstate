package com.brandPitara.sfs.provider.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Full replacement of a worker's typed charges; at most one entry per charge type. */
public record WorkerRatesReplaceRequest(
        @NotNull @Size(max = 5) List<@Valid @NotNull WorkerRateRequest> rates
) {
}
