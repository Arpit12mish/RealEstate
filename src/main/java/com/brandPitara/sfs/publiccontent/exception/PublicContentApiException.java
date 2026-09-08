package com.brandPitara.sfs.publiccontent.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PublicContentApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    private PublicContentApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static PublicContentApiException notFound() {
        return new PublicContentApiException(
                HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Published content was not found."
        );
    }

    public static PublicContentApiException temporarilyUnavailable() {
        return new PublicContentApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "CONTENT_TEMPORARILY_UNAVAILABLE",
                "Published content is temporarily unavailable."
        );
    }
}
