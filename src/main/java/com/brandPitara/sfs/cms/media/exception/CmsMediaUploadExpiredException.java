package com.brandPitara.sfs.cms.media.exception;

import org.springframework.http.HttpStatus;

public final class CmsMediaUploadExpiredException extends CmsMediaApiException {
    public CmsMediaUploadExpiredException() {
        super(HttpStatus.CONFLICT, "CMS_MEDIA_UPLOAD_EXPIRED", "The CMS media upload is no longer finalizable.");
    }
}
