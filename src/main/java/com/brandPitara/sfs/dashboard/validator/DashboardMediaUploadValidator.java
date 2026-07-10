package com.brandPitara.sfs.dashboard.validator;

import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import org.springframework.stereotype.Service;

@Service
public class DashboardMediaUploadValidator {

    private static final String IMAGE_JPEG       = "image/jpeg";
    private static final String IMAGE_JPG        = "image/jpg";
    private static final String IMAGE_PNG        = "image/png";
    private static final String IMAGE_WEBP       = "image/webp";
    private static final String VIDEO_MP4        = "video/mp4";
    private static final String APPLICATION_PDF  = "application/pdf";
    private static final String APPLICATION_JSON = "application/json";
    private static final long MAX_IMAGE_BYTES    = 2L * 1024 * 1024;
    private static final long MAX_PDF_BYTES      = 15L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES    = 5L * 1024 * 1024;
    private static final long MAX_JSON_BYTES     = 2L * 1024 * 1024;

    // Brand upload size limits (Phase 2B.2) - each brand upload type has its own limit,
    // distinct from the generic MAX_IMAGE_BYTES/MAX_VIDEO_BYTES used by pre-existing types.
    private static final long MAX_BRAND_LOGO_BYTES            = 2L * 1024 * 1024;
    private static final long MAX_BRAND_HERO_IMAGE_BYTES      = 5L * 1024 * 1024;
    private static final long MAX_BRAND_SKU_IMAGE_BYTES       = 3L * 1024 * 1024;
    private static final long MAX_BRAND_CERTIFICATE_BYTES     = 10L * 1024 * 1024;
    private static final long MAX_BRAND_PROMO_IMAGE_BYTES     = 5L * 1024 * 1024;
    private static final long MAX_BRAND_PROMO_VIDEO_BYTES     = 25L * 1024 * 1024;
    // Matches the dashboard's PresignedMediaUploadField, which hard-caps every non-video/
    // non-pdf client-side pre-check at 2 MB regardless of the maxSizeLabel a form passes in -
    // keep this at 2 MB so the client-side check and this server-side limit never disagree.
    private static final long MAX_BRAND_PRODUCT_CATEGORY_IMAGE_BYTES = 2L * 1024 * 1024;

    public void validateContentType(DashboardMediaUploadType uploadType, String contentType) {
        boolean isPdf = APPLICATION_PDF.equals(contentType);
        boolean isVideo = VIDEO_MP4.equals(contentType);
        boolean isSupportedImage = IMAGE_JPEG.equals(contentType)
                || IMAGE_JPG.equals(contentType)
                || IMAGE_PNG.equals(contentType)
                || IMAGE_WEBP.equals(contentType);

        if (uploadType.requiresPdf() && !isPdf) {
            throw new IllegalArgumentException("BROCHURE_PDF requires content type application/pdf");
        }
        if (uploadType.requiresImage() && isPdf) {
            throw new IllegalArgumentException(uploadType + " requires an image content type, not application/pdf");
        }
        if (uploadType.requiresImage() && !isSupportedImage) {
            throw new IllegalArgumentException(
                    uploadType + " supports only image/jpeg, image/jpg, image/png, image/webp"
            );
        }
        if (uploadType.requiresVideo() && !isVideo) {
            throw new IllegalArgumentException(uploadType + " supports only video/mp4");
        }
        if (uploadType.requiresLottieJson() && !APPLICATION_JSON.equals(contentType)) {
            throw new IllegalArgumentException(uploadType + " supports only application/json");
        }
        if (uploadType.allowsImageOrPdf() && !isPdf && !isSupportedImage) {
            throw new IllegalArgumentException(
                    uploadType + " supports only image/jpeg, image/jpg, image/png, image/webp, application/pdf"
            );
        }
        if (uploadType.allowsImageOrVideo() && !isVideo && !isSupportedImage) {
            throw new IllegalArgumentException(
                    uploadType + " supports only image/jpeg, image/jpg, image/png, image/webp, video/mp4"
            );
        }
    }

    public void validateFileSize(DashboardMediaUploadType uploadType, String contentType, long fileSizeBytes) {
        long limit = resolveSizeLimit(uploadType, contentType);
        if (fileSizeBytes > limit) {
            long limitMb = limit / (1024 * 1024);
            throw new IllegalArgumentException(
                uploadType + " exceeds maximum allowed size of " + limitMb + " MB"
            );
        }
    }

    private long resolveSizeLimit(DashboardMediaUploadType uploadType, String contentType) {
        if (uploadType == DashboardMediaUploadType.BRAND_LOGO) {
            return MAX_BRAND_LOGO_BYTES;
        }
        if (uploadType == DashboardMediaUploadType.BRAND_HERO_IMAGE) {
            return MAX_BRAND_HERO_IMAGE_BYTES;
        }
        if (uploadType == DashboardMediaUploadType.BRAND_SKU_IMAGE) {
            return MAX_BRAND_SKU_IMAGE_BYTES;
        }
        if (uploadType == DashboardMediaUploadType.BRAND_CERTIFICATE_FILE) {
            return MAX_BRAND_CERTIFICATE_BYTES;
        }
        if (uploadType == DashboardMediaUploadType.BRAND_PROMO_MEDIA) {
            return VIDEO_MP4.equals(contentType) ? MAX_BRAND_PROMO_VIDEO_BYTES : MAX_BRAND_PROMO_IMAGE_BYTES;
        }
        if (uploadType == DashboardMediaUploadType.BRAND_PRODUCT_CATEGORY_IMAGE) {
            return MAX_BRAND_PRODUCT_CATEGORY_IMAGE_BYTES;
        }
        if (uploadType.requiresPdf()) {
            return MAX_PDF_BYTES;
        }
        if (uploadType.requiresVideo()) {
            return MAX_VIDEO_BYTES;
        }
        if (uploadType.requiresLottieJson()) {
            return MAX_JSON_BYTES;
        }
        return MAX_IMAGE_BYTES;
    }

    public void validateContextIds(
            DashboardMediaUploadType uploadType,
            Long projectId,
            Long builderId,
            Long cityId,
            Long brandId,
            Long companyId
    ) {
        if (uploadType.isProjectScoped() && projectId == null) {
            throw new IllegalArgumentException(uploadType + " requires projectId");
        }
        if (uploadType.isBuilderScoped() && builderId == null) {
            throw new IllegalArgumentException(uploadType + " requires builderId");
        }
        if (uploadType.isCityScoped() && cityId == null) {
            throw new IllegalArgumentException(uploadType + " requires cityId");
        }
        if (uploadType.isBrandScoped() && brandId == null) {
            throw new IllegalArgumentException(uploadType + " requires brandId");
        }
        if (uploadType.isCompanyScoped() && companyId == null) {
            throw new IllegalArgumentException(uploadType + " requires companyId");
        }
    }
}
