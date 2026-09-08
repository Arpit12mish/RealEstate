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
                regexp = "^(image/(jpeg|jpg|png|webp)|application/pdf|video/mp4|application/json)$",
                message = "Allowed content types: image/jpeg, image/png, image/webp, application/pdf, video/mp4, application/json"
        )
        String contentType,

        @NotNull
        @Min(value = 1, message = "fileSizeBytes must be at least 1")
        Long fileSizeBytes,

        // Required when uploadType is PROJECT_IMAGE, FLOOR_PLAN_IMAGE, MASTER_PLAN_IMAGE, CONNECTIVITY_MAP, BROCHURE_PDF
        Long projectId,

        // Required when uploadType is BUILDER_LOGO or builder highlight media
        Long builderId,

        // Required when uploadType is CITY_COVER_IMAGE
        Long cityId,

        // Required when uploadType is a BRAND_* upload - the brand must already exist
        // (create-first-then-upload flow, no temp/pre-create upload path).
        Long brandId,

        // Required when uploadType is COMPANY_LOGO or COMPANY_COVER_IMAGE - the company must
        // already exist (same create-first-then-upload convention as BRAND_*).
        Long companyId,

        // Required when uploadType is HOME_PROMO_BANNER_VIDEO. The banner must
        // already exist, following the dashboard's create-entity-first upload convention.
        Long promoBannerId,

        // Required for COMPANY_PROJECT_MEDIA_IMAGE.
        Long companyProjectId
) {
    public DashboardPresignUploadRequest(
            DashboardMediaUploadType uploadType,
            String contentType,
            Long fileSizeBytes,
            Long projectId,
            Long builderId,
            Long cityId,
            Long brandId,
            Long companyId
    ) {
        this(uploadType, contentType, fileSizeBytes, projectId, builderId, cityId, brandId, companyId, null, null);
    }

    public DashboardPresignUploadRequest(
            DashboardMediaUploadType uploadType,
            String contentType,
            Long fileSizeBytes,
            Long projectId,
            Long builderId,
            Long cityId,
            Long brandId,
            Long companyId,
            Long promoBannerId
    ) {
        this(uploadType, contentType, fileSizeBytes, projectId, builderId, cityId, brandId, companyId, promoBannerId, null);
    }
}
