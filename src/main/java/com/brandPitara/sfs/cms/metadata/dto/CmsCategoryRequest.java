package com.brandPitara.sfs.cms.metadata.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

public record CmsCategoryRequest(@NotBlank @Size(max=150) String name, @Size(max=180) String slug,
                                 @Size(max=500) String description, Boolean active,
                                 @PositiveOrZero Long version) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown category field: " + field);
    }
}
