package com.brandPitara.sfs.cms.metadata.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record CmsAuthorRequest(
        @NotBlank @Size(max=150) String displayName,
        @Size(max=180) String slug,
        @Size(max=2000) String bio,
        @Size(max=150) String designation,
        Long profileMediaAssetId,
        Boolean active,
        @PositiveOrZero Long version
) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown author field: " + field);
    }
}
