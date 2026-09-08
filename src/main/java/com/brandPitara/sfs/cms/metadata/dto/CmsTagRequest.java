package com.brandPitara.sfs.cms.metadata.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record CmsTagRequest(@NotBlank @Size(max=100) String name, @Size(max=180) String slug,
                            Boolean active, @PositiveOrZero Long version) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown tag field: " + field);
    }
}
