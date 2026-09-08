package com.brandPitara.sfs.ratelimit.service.impl;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.model.RateLimitIdentity;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitIdentityResolver;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitIdentityEnforcementTest {

    @Test
    void tenAuthenticatedUsersBehindNatReceiveIndependentPrimaryAllowance() {
        Fixture fixture = fixture(1, 100);
        AtomicInteger allowed = new AtomicInteger();
        for (long id = 1; id <= 10; id++) {
            if (consume(fixture, user(id, "203.0.113.10"))) allowed.incrementAndGet();
        }
        assertThat(allowed).hasValue(10);
        assertThat(fixture.service.bucketCache().estimatedSize()).isEqualTo(10);
        assertThat(fixture.service.abuseBucketCache().estimatedSize()).isEqualTo(1);
    }

    @Test
    void tenValidatedGuestsBehindNatReceiveIndependentPrimaryAllowance() {
        Fixture fixture = fixture(1, 100);
        AtomicInteger allowed = new AtomicInteger();
        for (long id = 1; id <= 10; id++) {
            if (consume(fixture, guest(id, "203.0.113.11"))) allowed.incrementAndGet();
        }
        assertThat(allowed).hasValue(10);
        assertThat(fixture.service.bucketCache().estimatedSize()).isEqualTo(10);
        assertThat(fixture.service.abuseBucketCache().estimatedSize()).isEqualTo(1);
    }

    @Test
    void oneUserAcrossIpsStillSharesPrimaryAllowance() {
        Fixture fixture = fixture(2, 100);
        assertThat(consume(fixture, user(42, "198.51.100.1"))).isTrue();
        assertThat(consume(fixture, user(42, "198.51.100.2"))).isTrue();
        assertThat(consume(fixture, user(42, "198.51.100.3"))).isFalse();
    }

    @Test
    void independentIpAbuseBucketStopsManyPrincipalsAndSurvivesPrimaryEviction() {
        Fixture fixture = fixture(100, 3);
        for (long id = 1; id <= 3; id++) assertThat(consume(fixture, user(id, "192.0.2.8"))).isTrue();
        assertThat(consume(fixture, user(4, "192.0.2.8"))).isFalse();

        for (long id = 10; id < 2_000; id++) consume(fixture, user(id, "198.51.100." + (id % 250 + 1)));
        fixture.service.bucketCache().cleanUp();
        assertThat(fixture.service.bucketCache().estimatedSize()).isLessThanOrEqualTo(16);
        assertThat(consume(fixture, user(5, "192.0.2.8"))).isFalse();
    }

    private boolean consume(Fixture fixture, RateLimitIdentity identity) {
        return fixture.service.checkAndConsume(RateLimitPolicy.PUBLIC_HOME_READ, Map.of(
                RateLimitKeyType.PRIMARY_IDENTITY, identity.primaryKey(),
                RateLimitKeyType.IP_ABUSE, identity.abuseIp())).allowed();
    }

    private RateLimitIdentity user(long id, String ip) {
        MockHttpServletRequest request = request(ip);
        request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE, RateLimitAuthenticationAttributes.STATE_USER);
        request.setAttribute(RateLimitAuthenticationAttributes.USER_ID, id);
        return resolver().resolve(request);
    }

    private RateLimitIdentity guest(long id, String ip) {
        MockHttpServletRequest request = request(ip);
        request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE, RateLimitAuthenticationAttributes.STATE_GUEST);
        request.setAttribute(RateLimitAuthenticationAttributes.GUEST_SESSION_ID, id);
        return resolver().resolve(request);
    }

    private Fixture fixture(long primaryCapacity, long abuseCapacity) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getBucketCache().setPrimaryMaximumSize(16);
        properties.getBucketCache().setAbuseMaximumSize(1_000);
        RateLimitProperties.PolicyConfig policy = new RateLimitProperties.PolicyConfig();
        policy.setLimits(List.of(limit(RateLimitKeyType.PRIMARY_IDENTITY, primaryCapacity),
                limit(RateLimitKeyType.IP_ABUSE, abuseCapacity)));
        properties.getPolicies().put(RateLimitPolicy.PUBLIC_HOME_READ, policy);
        return new Fixture(new InMemoryRateLimitService(properties));
    }

    private RateLimitProperties.LimitConfig limit(RateLimitKeyType type, long capacity) {
        RateLimitProperties.LimitConfig limit = new RateLimitProperties.LimitConfig();
        limit.setKeyType(type);
        limit.setCapacity(capacity);
        limit.setRefillTokens(capacity);
        limit.setRefillPeriodSeconds(60);
        return limit;
    }

    private RateLimitIdentityResolver resolver() {
        return new RateLimitIdentityResolver(new ClientIpResolver(new RateLimitProperties()));
    }

    private MockHttpServletRequest request(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/home");
        request.setRemoteAddr(ip);
        return request;
    }

    private record Fixture(InMemoryRateLimitService service) {}
}
