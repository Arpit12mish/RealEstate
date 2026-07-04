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
}
