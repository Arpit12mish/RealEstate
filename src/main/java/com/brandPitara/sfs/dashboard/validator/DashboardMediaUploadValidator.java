package com.brandPitara.sfs.dashboard.validator;

import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import org.springframework.stereotype.Service;

@Service
public class DashboardMediaUploadValidator {

    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String IMAGE_JPG = "image/jpg";
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_WEBP = "image/webp";
    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;
    private static final long MAX_PDF_BYTES   = 15L * 1024 * 1024;

    public void validateContentType(DashboardMediaUploadType uploadType, String contentType) {
        boolean isPdf = "application/pdf".equals(contentType);
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
        if (uploadType == DashboardMediaUploadType.CITY_COVER_IMAGE && !isSupportedImage) {
            throw new IllegalArgumentException(
                    "CITY_COVER_IMAGE supports only image/jpeg, image/jpg, image/png, image/webp"
            );
        }
    }

    public void validateFileSize(DashboardMediaUploadType uploadType, long fileSizeBytes) {
        long limit = uploadType.requiresPdf() ? MAX_PDF_BYTES : MAX_IMAGE_BYTES;
        if (fileSizeBytes > limit) {
            long limitMb = limit / (1024 * 1024);
            throw new IllegalArgumentException(
                uploadType + " exceeds maximum allowed size of " + limitMb + " MB"
            );
        }
    }

    public void validateContextIds(DashboardMediaUploadType uploadType, Long projectId, Long builderId, Long cityId) {
        if (uploadType.isProjectScoped() && projectId == null) {
            throw new IllegalArgumentException(uploadType + " requires projectId");
        }
        if (uploadType.isBuilderScoped() && builderId == null) {
            throw new IllegalArgumentException(uploadType + " requires builderId");
        }
        if (uploadType.isCityScoped() && cityId == null) {
            throw new IllegalArgumentException(uploadType + " requires cityId");
        }
    }
}
