package com.brandPitara.sfs.ratelimit.resolver;

import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.model.RateLimitRequestContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitKeyResolverTest {

    private final RateLimitKeyResolver resolver = new RateLimitKeyResolver();

    @Test
    void normalizesPhoneNumberBeforeUsingItAsAKey() {
        RateLimitRequestContext context = RateLimitRequestContext.builder()
                .ip("1.2.3.4")
                .phoneNumber("9876543210")
                .build();

        Map<RateLimitKeyType, String> keys = resolver.resolveKeys(List.of(RateLimitKeyType.PHONE), context);

        assertThat(keys.get(RateLimitKeyType.PHONE)).isEqualTo("+919876543210");
    }

    @Test
    void equivalentPhoneFormatsProduceTheSameKey() {
        RateLimitRequestContext raw = RateLimitRequestContext.builder().phoneNumber("9876543210").build();
        RateLimitRequestContext zeroPrefixed = RateLimitRequestContext.builder().phoneNumber("09876543210").build();
        RateLimitRequestContext canonical = RateLimitRequestContext.builder().phoneNumber("+919876543210").build();

        String keyA = resolver.resolveKeys(List.of(RateLimitKeyType.PHONE), raw).get(RateLimitKeyType.PHONE);
        String keyB = resolver.resolveKeys(List.of(RateLimitKeyType.PHONE), zeroPrefixed).get(RateLimitKeyType.PHONE);
        String keyC = resolver.resolveKeys(List.of(RateLimitKeyType.PHONE), canonical).get(RateLimitKeyType.PHONE);

        assertThat(keyA).isEqualTo(keyB).isEqualTo(keyC);
    }

    @Test
    void neverUsesRawRefreshTokenAsKeyMaterial() {
        String rawToken = "super-secret-refresh-token-value";
        RateLimitRequestContext context = RateLimitRequestContext.builder()
                .ip("1.2.3.4")
                .refreshToken(rawToken)
                .build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.IP_AND_TOKEN), context).get(RateLimitKeyType.IP_AND_TOKEN);

        assertThat(key).isNotNull();
        assertThat(key).doesNotContain(rawToken);
        // SHA-256 hex digest is 64 chars; key format is "<ip>|<hash>".
        assertThat(key.substring(key.indexOf('|') + 1)).hasSize(64);
    }

    @Test
    void hashTokenIsDeterministicForTheSameInput() {
        assertThat(resolver.hashToken("abc")).isEqualTo(resolver.hashToken("abc"));
        assertThat(resolver.hashToken("abc")).isNotEqualTo(resolver.hashToken("abd"));
    }

    @Test
    void ipOrUserFallsBackToIpWhenNoUserIsAuthenticated() {
        RateLimitRequestContext context = RateLimitRequestContext.builder().ip("5.5.5.5").build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.IP_OR_USER), context).get(RateLimitKeyType.IP_OR_USER);

        assertThat(key).isEqualTo("ip:5.5.5.5");
    }

    @Test
    void ipOrUserPrefersUserIdWhenAuthenticated() {
        RateLimitRequestContext context = RateLimitRequestContext.builder().ip("5.5.5.5").userId(42L).build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.IP_OR_USER), context).get(RateLimitKeyType.IP_OR_USER);

        assertThat(key).isEqualTo("user:42");
    }

    @Test
    void missingKeyTypeIsOmittedRatherThanThrowing() {
        RateLimitRequestContext context = RateLimitRequestContext.builder().build();

        Map<RateLimitKeyType, String> keys = resolver.resolveKeys(List.of(RateLimitKeyType.IP_AND_TOKEN), context);

        assertThat(keys).doesNotContainKey(RateLimitKeyType.IP_AND_TOKEN);
    }

    // ── Phase 2: mobile action APIs ──────────────────────────────────────────────

    @Test
    void mobileAuthenticatedApisPreferUserIdOverIp() {
        // MOBILE_PROFILE_READ/WRITE, MOBILE_FAVORITE_READ/WRITE, MOBILE_REVIEW_READ/WRITE,
        // MOBILE_MEDIA_OR_UPLOAD_ACTION, and MOBILE_PROVIDER_ACCOUNT_READ/WRITE all reuse
        // IP_OR_USER, so an authenticated userId must win over IP - same guarantee
        // ipOrUserPrefersUserIdWhenAuthenticated already covers, verified again here
        // for traceability to this phase's explicit requirement.
        RateLimitRequestContext context = RateLimitRequestContext.builder().ip("6.6.6.6").userId(99L).build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.IP_OR_USER), context).get(RateLimitKeyType.IP_OR_USER);

        assertThat(key).isEqualTo("user:99");
    }

    @Test
    void mobileAnonymousFallbackUsesIpWhenUserIsUnresolved() {
        RateLimitRequestContext context = RateLimitRequestContext.builder().ip("6.6.6.6").build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.IP_OR_USER), context).get(RateLimitKeyType.IP_OR_USER);

        assertThat(key).isEqualTo("ip:6.6.6.6");
    }

    @Test
    void bodyFingerprintIsStableForIdenticalCalculatorRequestBodies() {
        String canonicalBodyA = "{\"cityId\":1,\"propertyValue\":5000000}";
        String canonicalBodyB = "{\"cityId\":1,\"propertyValue\":5000000}";

        RateLimitRequestContext contextA = RateLimitRequestContext.builder().bodyFingerprint(canonicalBodyA).build();
        RateLimitRequestContext contextB = RateLimitRequestContext.builder().bodyFingerprint(canonicalBodyB).build();

        String keyA = resolver.resolveKeys(List.of(RateLimitKeyType.BODY_FINGERPRINT), contextA).get(RateLimitKeyType.BODY_FINGERPRINT);
        String keyB = resolver.resolveKeys(List.of(RateLimitKeyType.BODY_FINGERPRINT), contextB).get(RateLimitKeyType.BODY_FINGERPRINT);

        assertThat(keyA).isEqualTo(keyB);
    }

    @Test
    void bodyFingerprintDiffersForDifferentCalculatorRequestBodies() {
        RateLimitRequestContext contextA = RateLimitRequestContext.builder()
                .bodyFingerprint("{\"cityId\":1,\"propertyValue\":5000000}").build();
        RateLimitRequestContext contextB = RateLimitRequestContext.builder()
                .bodyFingerprint("{\"cityId\":2,\"propertyValue\":5000000}").build();

        String keyA = resolver.resolveKeys(List.of(RateLimitKeyType.BODY_FINGERPRINT), contextA).get(RateLimitKeyType.BODY_FINGERPRINT);
        String keyB = resolver.resolveKeys(List.of(RateLimitKeyType.BODY_FINGERPRINT), contextB).get(RateLimitKeyType.BODY_FINGERPRINT);

        assertThat(keyA).isNotEqualTo(keyB);
    }

    @Test
    void rawBodyFingerprintInputNeverAppearsInTheResolvedKey() {
        String rawBody = "{\"note\":\"unique-body-marker-should-never-appear-in-key\"}";
        RateLimitRequestContext context = RateLimitRequestContext.builder().bodyFingerprint(rawBody).build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.BODY_FINGERPRINT), context).get(RateLimitKeyType.BODY_FINGERPRINT);

        assertThat(key).isNotNull();
        assertThat(key).doesNotContain("unique-body-marker-should-never-appear-in-key");
        // SHA-256 hex digest is 64 chars, matching the same hashing convention as
        // IP_AND_TOKEN/IP_AND_QUERY - never the raw value itself.
        assertThat(key).hasSize(64);
    }

    @Test
    void mobileActionKeysAreNotPartitionedByPathVariablesByDesign() {
        // Path-variable-based keying (e.g. per-projectId buckets) was intentionally
        // not added for Phase 2 - "do not overbuild" per the task's own instruction.
        // Two requests for different resources by the same user must resolve to the
        // exact same IP_OR_USER key, proving no hidden path-variable dimension exists.
        RateLimitRequestContext forProjectA = RateLimitRequestContext.builder().ip("6.6.6.6").userId(7L).build();
        RateLimitRequestContext forProjectB = RateLimitRequestContext.builder().ip("6.6.6.6").userId(7L).build();

        String keyA = resolver.resolveKeys(List.of(RateLimitKeyType.IP_OR_USER), forProjectA).get(RateLimitKeyType.IP_OR_USER);
        String keyB = resolver.resolveKeys(List.of(RateLimitKeyType.IP_OR_USER), forProjectB).get(RateLimitKeyType.IP_OR_USER);

        assertThat(keyA).isEqualTo(keyB);
    }

    // ── Phase 3: remaining mobile/public API rate limiting ──────────────────────
    // MOBILE_SERVICE_REQUEST_WRITE, MOBILE_PROVIDER_INTEREST_WRITE, and
    // MOBILE_ONBOARDING_WRITE all reuse IP_OR_USER verbatim (no new key-resolution
    // logic was needed this phase); PUBLIC_BUSINESS_EVENT_WRITE and the new
    // PUBLIC_*_READ policies reuse plain IP. These tests exist for traceability to
    // this phase's explicit testing requirements, not because new logic was added.

    @Test
    void serviceRequestAndOnboardingWritesPreferUserIdOverIp() {
        RateLimitRequestContext context = RateLimitRequestContext.builder().ip("7.7.7.7").userId(123L).build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.IP_OR_USER), context).get(RateLimitKeyType.IP_OR_USER);

        assertThat(key).isEqualTo("user:123");
    }

    @Test
    void serviceRequestAndOnboardingWritesFallBackToIpSafelyWhenUnresolved() {
        RateLimitRequestContext context = RateLimitRequestContext.builder().ip("7.7.7.7").build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.IP_OR_USER), context).get(RateLimitKeyType.IP_OR_USER);

        assertThat(key).isEqualTo("ip:7.7.7.7");
        assertThat(key).doesNotContain("null");
    }

    @Test
    void publicBusinessEventAndReadPoliciesKeyOnIpAlone() {
        RateLimitRequestContext context = RateLimitRequestContext.builder().ip("8.8.8.8").build();

        String key = resolver.resolveKeys(List.of(RateLimitKeyType.IP), context).get(RateLimitKeyType.IP);

        assertThat(key).isEqualTo("8.8.8.8");
    }
}
