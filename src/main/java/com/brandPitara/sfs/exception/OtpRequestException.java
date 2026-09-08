package com.brandPitara.sfs.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Structured domain error for OTP send/resend failures (cooldown, rate/abuse
 * limits, provider unavailability). Extends ResponseStatusException (rather
 * than a plain RuntimeException) so existing call sites/tests that assert on
 * ResponseStatusException/getStatusCode() keep working unchanged; Spring
 * still dispatches it to OtpRequestExceptionHandler in preference to
 * GlobalExceptionHandler's generic ResponseStatusException handler, since
 * that resolves to the most specific @ExceptionHandler match. The added
 * {@code code} and {@code retryAfterSeconds} give the frontend a
 * machine-readable contract instead of having to parse a free-text message.
 */
@Getter
public class OtpRequestException extends ResponseStatusException {

    private final String code;
    private final Long retryAfterSeconds;

    public OtpRequestException(HttpStatus status, String code, String message, Long retryAfterSeconds) {
        super(status, message);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
