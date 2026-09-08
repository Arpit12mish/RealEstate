package com.brandPitara.sfs.cms.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestChangesRequest(
        @NotNull Long version,
        @NotBlank @Size(max = 4000) String comment
) {
}
