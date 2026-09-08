package com.brandPitara.sfs.cdn.gateway;

public class CdnInvalidationException extends RuntimeException {
    public CdnInvalidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
