package com.brandPitara.sfs.dashboard.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creates a dealer listing; all further fields are set with {@link DealerUpdateRequest}. */
public record DealerCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull Long categoryId,
        @NotNull Long cityId
) {
}
