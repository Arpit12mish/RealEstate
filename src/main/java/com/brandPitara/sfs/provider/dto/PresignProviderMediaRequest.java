package com.brandPitara.sfs.provider.dto;

import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PresignProviderMediaRequest(
        @NotNull ProviderMediaType mediaType,
        @NotNull
        @Pattern(regexp = "^image/(jpeg|jpg|png|webp)$",
                message = "Only image/jpeg, image/png, image/webp allowed")
        String contentType
) {}
