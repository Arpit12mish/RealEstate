package com.brandPitara.sfs.dashboard.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Full replacement of the dealer's PRODUCT or SERVICE groups (the type is in the path). */
public record OfferingsReplaceRequest(
        @NotNull @Size(max = 20) List<@Valid @NotNull Group> groups
) {
    public record Group(
            @NotBlank @Size(max = 120) String title,
            @NotNull @Size(min = 1, max = 40) List<@NotBlank @Size(max = 120) String> items
    ) {
    }
}
