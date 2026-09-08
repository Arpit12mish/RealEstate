package com.brandPitara.sfs.security.identity;

import com.brandPitara.sfs.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MobileAuthenticationIdentityCache {

    private static final String NAMESPACE = "mobile";

    private final UserRepository userRepository;
    private final AuthenticationIdentityCacheProperties.Namespace properties;
    private final Cache<Long, MobileAuthenticationUserSnapshot> cache;
    private final Counter loads;
    private final Counter invalidations;
    private final Counter hits;
    private final Counter misses;
    private final Counter disabledRequests;
    private final Counter loadFailures;

    @Autowired
    public MobileAuthenticationIdentityCache(
            UserRepository userRepository,
            AuthenticationIdentityCacheProperties properties,
            MeterRegistry meterRegistry
    ) {
        this(userRepository, properties.getMobile(), meterRegistry, Ticker.systemTicker());
    }

    MobileAuthenticationIdentityCache(
            UserRepository userRepository,
            AuthenticationIdentityCacheProperties.Namespace properties,
            MeterRegistry meterRegistry,
            Ticker ticker
    ) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.cache = Caffeine.newBuilder()
                .maximumSize(properties.getMaximumSize())
                .expireAfterWrite(properties.getExpireAfterWrite())
                .ticker(ticker)
                .recordStats()
                .build();
        this.loads = counter(meterRegistry, "loads", "success");
        this.invalidations = counter(meterRegistry, "invalidations", "explicit");
        this.hits = counter(meterRegistry, "requests", "hit");
        this.misses = counter(meterRegistry, "requests", "miss");
        this.disabledRequests = counter(meterRegistry, "requests", "disabled");
        this.loadFailures = counter(meterRegistry, "loads", "failure");
        Gauge.builder("sfs.authentication.identity.cache.size", cache, Cache::estimatedSize)
                .tag("namespace", NAMESPACE)
                .register(meterRegistry);
    }

    public MobileAuthenticationUserSnapshot get(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (!properties.isEnabled()) {
            disabledRequests.increment();
            return loadWithFailureMetric(userId);
        }
        MobileAuthenticationUserSnapshot cached = cache.getIfPresent(userId);
        if (cached != null) {
            hits.increment();
            return cached;
        }
        misses.increment();
        return cache.get(userId, this::loadWithFailureMetric);
    }

    public void invalidate(Long userId) {
        if (userId == null) {
            return;
        }
        cache.invalidate(userId);
        invalidations.increment();
    }

    public long estimatedSize() {
        cache.cleanUp();
        return cache.estimatedSize();
    }

    private MobileAuthenticationUserSnapshot load(Long userId) {
        MobileAuthenticationUserSnapshot snapshot = userRepository.findAuthenticationSnapshotById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Mobile user not found"));
        if (!snapshot.enabled()) {
            throw new DisabledException("Mobile user is disabled");
        }
        loads.increment();
        return snapshot;
    }

    private MobileAuthenticationUserSnapshot loadWithFailureMetric(Long userId) {
        try {
            return load(userId);
        } catch (RuntimeException ex) {
            loadFailures.increment();
            throw ex;
        }
    }

    private Counter counter(MeterRegistry registry, String operation, String outcome) {
        return Counter.builder("sfs.authentication.identity.cache.operations")
                .tag("namespace", NAMESPACE)
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(registry);
    }
}
