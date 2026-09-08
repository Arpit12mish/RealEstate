package com.brandPitara.sfs.cms.media.dto;

import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import jakarta.validation.constraints.*;

public record CmsMediaUploadRequest(
        @NotNull CmsMediaType mediaType,
        @NotBlank @Size(max = 255) String filename,
        @NotBlank @Size(max = 100) String contentType,
        @NotNull @Positive Long sizeBytes
) {
}
