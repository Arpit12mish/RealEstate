package com.brandPitara.sfs.integration;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Safe, category-specific failure for synchronous third-party provider calls.
 * Messages intentionally exclude provider response bodies, URLs and credentials.
 */
public final class ExternalProviderException extends ResponseStatusException {

    private ExternalProviderException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }

    public static ExternalProviderException unavailable(String provider, String reason) {
        return new ExternalProviderException(
            HttpStatus.SERVICE_UNAVAILABLE,
            provider + " is unavailable: " + reason,
            null
        );
    }

    public static ExternalProviderException timeout(String provider, Throwable cause) {
        return new ExternalProviderException(
            HttpStatus.GATEWAY_TIMEOUT,
            provider + " request timed out",
            cause
        );
    }

    public static ExternalProviderException upstreamFailure(String provider, Throwable cause) {
        return new ExternalProviderException(
            HttpStatus.BAD_GATEWAY,
            provider + " request failed",
            cause
        );
    }

    public static ExternalProviderException upstreamStatus(String provider, int statusCode) {
        return new ExternalProviderException(
            HttpStatus.BAD_GATEWAY,
            provider + " returned an unsuccessful response (status " + statusCode + ")",
            null
        );
    }
}
