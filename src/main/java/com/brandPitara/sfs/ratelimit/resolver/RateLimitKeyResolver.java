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
 * Builds bucket key material for each configured dimension. Legacy composite
 * key types are intentionally collapsed to the already-validated primary
 * identity; client tokens, installation/device IDs and query values never
 * affect cache cardinality.
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
            case PRIMARY_IDENTITY -> java.util.Optional.ofNullable(ctx.getPrimaryIdentity());
            case IP_ABUSE -> java.util.Optional.ofNullable(ctx.getAbuseIp());
            case PHONE -> java.util.Optional.ofNullable(normalizedPhoneOrRaw(ctx.getPhoneNumber()));
            case IP -> java.util.Optional.ofNullable(ctx.getIp());
            case IP_AND_TOKEN, IP_AND_INSTALLATION, IP_AND_DEVICE, IP_AND_QUERY -> primary(ctx);
            case IP_OR_USER -> ctx.getPrimaryIdentity() != null
                    ? java.util.Optional.of(ctx.getPrimaryIdentity())
                    : ctx.getUserId() != null
                            ? java.util.Optional.of("user:" + ctx.getUserId())
                            : java.util.Optional.ofNullable(ctx.getIp()).map(ip -> "ip:" + ip);
            // A request with no usable body must not share one global bucket with every other
            // such request (same reasoning as PHONE above, and the same real incident class:
            // one attacker sending empty-body requests would otherwise exhaust a bucket every
            // other client with a blank/unparsable body also draws from). Omit the dimension
            // instead - every other configured dimension (e.g. PRIMARY_IDENTITY) still applies.
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
            // Malformed phone: let the real controller validation reject it. Return
            // null rather than a shared literal so this dimension is simply omitted
            // (per this class's documented contract) - a request with no usable
            // phone must not share one global bucket with every other such request,
            // which would let one attacker exhaust it and block unrelated clients.
            // The PRIMARY_IDENTITY dimension configured alongside PHONE on every
            // OTP policy still applies.
            return null;
        }
    }

    private java.util.Optional<String> primary(RateLimitRequestContext context) {
        return java.util.Optional.ofNullable(
                context.getPrimaryIdentity() != null ? context.getPrimaryIdentity() : context.getIp());
    }

    private String sha256(String value) {
        if (value == null || value.isBlank()) {
            return MISSING_MARKER;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
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
        return sha256(normalized);
    }
}
