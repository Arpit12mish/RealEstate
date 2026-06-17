package com.brandPitara.sfs.dashboard.media.dto;

import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DashboardPresignUploadRequest(

        @NotNull
        DashboardMediaUploadType uploadType,

        @NotBlank
        @Pattern(
                regexp = "^(image/(jpeg|jpg|png|webp)|application/pdf)$",
                message = "Allowed content types: image/jpeg, image/png, image/webp, application/pdf"
        )
        String contentType,

        @NotNull
        @Min(value = 1, message = "fileSizeBytes must be at least 1")
        Long fileSizeBytes,

        // Required when uploadType is PROJECT_IMAGE, FLOOR_PLAN_IMAGE, CONNECTIVITY_MAP, BROCHURE_PDF
        Long projectId,

        // Required when uploadType is BUILDER_LOGO
        Long builderId,

        // Required when uploadType is CITY_COVER_IMAGE
        Long cityId
) {}
