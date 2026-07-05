package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.model.RateLimitRequestContext;
import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the raw key material Bucket4j buckets are keyed on, for each
 * RateLimitKeyType a policy's limits reference. Never returns raw phone
 * numbers, refresh tokens, or OTPs into a bucket key that could later be
 * logged verbatim - phone numbers are normalized (already the canonical,
 * non-secret account identifier) and tokens are always SHA-256 hashed first.
 */
@Component
public class RateLimitKeyResolver {

    private static final String MISSING_MARKER = "_none_";

    /**
     * Resolves the raw key value for every key type present in
     * {@code requiredKeyTypes}. A key type that cannot be resolved for this
     * request (e.g. no authenticated user for IP_OR_USER) is simply omitted,
     * letting the caller fail open for that dimension only.
     */
    public Map<RateLimitKeyType, String> resolveKeys(
            Iterable<RateLimitKeyType> requiredKeyTypes,
            RateLimitRequestContext context
    ) {
        Map<RateLimitKeyType, String> resolved = new EnumMap<>(RateLimitKeyType.class);
        for (RateLimitKeyType keyType : requiredKeyTypes) {
            resolveSingle(keyType, context).ifPresent(value -> resolved.put(keyType, value));
        }
        return resolved;
    }

    private java.util.Optional<String> resolveSingle(RateLimitKeyType keyType, RateLimitRequestContext ctx) {
        return switch (keyType) {
            case PHONE -> java.util.Optional.ofNullable(normalizedPhoneOrRaw(ctx.getPhoneNumber()));
            case IP -> java.util.Optional.ofNullable(ctx.getIp());
            case IP_AND_TOKEN -> ctx.getIp() == null
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(ctx.getIp() + "|" + hashToken(ctx.getRefreshToken()));
            case IP_AND_INSTALLATION -> ctx.getIp() == null
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(ctx.getIp() + "|" + orMissing(ctx.getInstallationId()));
            case IP_OR_USER -> ctx.getUserId() != null
                    ? java.util.Optional.of("user:" + ctx.getUserId())
                    : java.util.Optional.ofNullable(ctx.getIp()).map(ip -> "ip:" + ip);
            case IP_AND_DEVICE -> ctx.getIp() == null
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(ctx.getIp() + "|" + orMissing(ctx.getDeviceId()));
            case IP_AND_QUERY -> ctx.getIp() == null
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(ctx.getIp() + "|" + fingerprint(ctx.getQuery()));
            case BODY_FINGERPRINT -> ctx.getBodyFingerprint() == null || ctx.getBodyFingerprint().isBlank()
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(fingerprint(ctx.getBodyFingerprint()));
        };
    }

    private String normalizedPhoneOrRaw(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        try {
            return PhoneNumberNormalizer.normalize(rawPhone);
        } catch (ResponseStatusException ex) {
            // Let the real controller validation reject a malformed phone number;
            // the rate limiter still needs a stable-ish key for the attempt itself.
            return "raw:" + rawPhone.trim().toLowerCase(Locale.ROOT);
        }
    }

    /** SHA-256 hex digest. The raw token is never retained, logged, or used as a key directly. */
    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return MISSING_MARKER;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash refresh token for rate limiting", ex);
        }
    }

    /** Shared by IP_AND_QUERY's search-query normalization and BODY_FINGERPRINT's canonical body JSON. */
    private String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return MISSING_MARKER;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return hashToken(normalized);
    }

    private String orMissing(String value) {
        return (value == null || value.isBlank()) ? MISSING_MARKER : value;
    }
}
