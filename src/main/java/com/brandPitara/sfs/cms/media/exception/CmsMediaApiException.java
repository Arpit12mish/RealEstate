package com.brandPitara.sfs.cms.media.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public class CmsMediaApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public CmsMediaApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static CmsMediaApiException notFound(Long id) {
        return new CmsMediaApiException(HttpStatus.NOT_FOUND, "CMS_MEDIA_NOT_FOUND", "CMS media asset not found: " + id);
    }

    public static CmsMediaApiException invalidType(String message) {
        return new CmsMediaApiException(HttpStatus.BAD_REQUEST, "CMS_MEDIA_INVALID_TYPE", message);
    }

    public static CmsMediaApiException tooLarge(long limit) {
        return new CmsMediaApiException(HttpStatus.CONTENT_TOO_LARGE, "CMS_MEDIA_TOO_LARGE",
                "CMS media exceeds the maximum allowed size of " + limit + " bytes.");
    }

    public static CmsMediaApiException validationFailed() {
        return new CmsMediaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CMS_MEDIA_VALIDATION_FAILED",
                "Uploaded object failed CMS media validation.");
    }

    public static CmsMediaApiException uploadIncomplete() {
        return new CmsMediaApiException(HttpStatus.CONFLICT, "CMS_MEDIA_UPLOAD_INCOMPLETE",
                "The expected upload object is not available yet.");
    }

    public static CmsMediaApiException notReady() {
        return new CmsMediaApiException(HttpStatus.CONFLICT, "CMS_MEDIA_NOT_READY", "CMS media is not ready for preview.");
    }

    public static CmsMediaApiException storageUnavailable() {
        return new CmsMediaApiException(HttpStatus.SERVICE_UNAVAILABLE, "CMS_MEDIA_STORAGE_UNAVAILABLE",
                "CMS media storage is temporarily unavailable. Please retry.");
    }
}
