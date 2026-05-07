package com.brandPitara.sfs.dashboard.validator;

import com.brandPitara.sfs.dashboard.media.enums.DashboardMediaUploadType;
import org.springframework.stereotype.Service;

@Service
public class DashboardMediaUploadValidator {

    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;
    private static final long MAX_PDF_BYTES   = 15L * 1024 * 1024;

    public void validateContentType(DashboardMediaUploadType uploadType, String contentType) {
        boolean isPdf = "application/pdf".equals(contentType);

        if (uploadType.requiresPdf() && !isPdf) {
            throw new IllegalArgumentException("BROCHURE_PDF requires content type application/pdf");
        }
        if (uploadType.requiresImage() && isPdf) {
            throw new IllegalArgumentException(uploadType + " requires an image content type, not application/pdf");
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

    public void validateContextIds(DashboardMediaUploadType uploadType, Long projectId, Long builderId) {
        if (uploadType.isProjectScoped() && projectId == null) {
            throw new IllegalArgumentException(uploadType + " requires projectId");
        }
        if (uploadType.isBuilderScoped() && builderId == null) {
            throw new IllegalArgumentException(uploadType + " requires builderId");
        }
    }
}
