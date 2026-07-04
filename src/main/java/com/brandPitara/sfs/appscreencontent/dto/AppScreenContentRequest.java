package com.brandPitara.sfs.appscreencontent.dto;

import com.brandPitara.sfs.appscreencontent.enums.AppScreenKey;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenMediaType;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenPlacement;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record AppScreenContentRequest(
        @NotNull AppScreenKey screenKey,
        @NotNull AppScreenPlacement placement,
        @NotNull AppScreenMediaType mediaType,

        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "^https?://.+", message = "mediaUrl must be an http(s) URL")
        String mediaUrl,

        Boolean enabled,

        @Pattern(
                regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$",
                message = "backgroundColor must be a hex color like #000000"
        )
        String backgroundColor,

        @DecimalMin(value = "0.1", message = "aspectRatio must be greater than 0")
        @DecimalMax(value = "10.0", message = "aspectRatio must be reasonable")
        Double aspectRatio,

        OffsetDateTime startAt,
        OffsetDateTime endAt,

        @Size(max = 40)
        String minAppVersion,

        Integer sortOrder
) {}
