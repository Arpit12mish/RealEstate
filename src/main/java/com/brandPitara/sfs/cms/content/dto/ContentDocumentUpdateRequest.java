package com.brandPitara.sfs.cms.content.dto;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ContentDocumentUpdateRequest(
        @NotNull @PositiveOrZero Long version,
        @NotNull @Valid ContentDocument document
) {
    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object ignored) {
        throw new IllegalArgumentException("Unknown document-update field: " + property);
    }
}
