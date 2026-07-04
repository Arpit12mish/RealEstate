package com.brandPitara.sfs.ratelimit.exception;

/**
 * Thrown by CachedBodyHttpServletRequest when a request body exceeds
 * {@code sfs.rate-limit.max-cached-body-bytes} while RateLimitingFilter is
 * buffering it to extract key material. The filter catches this and responds
 * 413 before the real controller ever executes, so an oversized body is
 * never fully buffered in memory.
 */
public class RequestBodyTooLargeException extends RuntimeException {
    public RequestBodyTooLargeException(long maxBodyBytes) {
        super("Request body exceeds the maximum size of " + maxBodyBytes + " bytes allowed for rate-limit key extraction");
    }
}
