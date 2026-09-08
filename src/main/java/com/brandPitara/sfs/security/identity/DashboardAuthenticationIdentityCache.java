package com.brandPitara.sfs.security.identity;

import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DashboardAuthenticationIdentityCache {

    private static final String NAMESPACE = "dashboard";

    private final DashboardUserRepository userRepository;
    private final AuthenticationIdentityCacheProperties.Namespace properties;
    private final Cache<Long, DashboardAuthenticationUserSnapshot> cache;
    private final Counter loads;
    private final Counter invalidations;
    private final Counter hits;
    private final Counter misses;
    private final Counter disabledRequests;
    private final Counter loadFailures;

    @Autowired
    public DashboardAuthenticationIdentityCache(
            DashboardUserRepository userRepository,
            AuthenticationIdentityCacheProperties properties,
            MeterRegistry meterRegistry
    ) {
        this(userRepository, properties.getDashboard(), meterRegistry, Ticker.systemTicker());
    }

    DashboardAuthenticationIdentityCache(
            DashboardUserRepository userRepository,
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

    public DashboardAuthenticationUserSnapshot get(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (!properties.isEnabled()) {
            disabledRequests.increment();
            return loadWithFailureMetric(userId);
        }
        DashboardAuthenticationUserSnapshot cached = cache.getIfPresent(userId);
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

    private DashboardAuthenticationUserSnapshot load(Long userId) {
        List<DashboardAuthenticationUserRow> rows = userRepository.findAuthenticationRowsById(userId);
        if (rows.isEmpty()) {
            throw new UsernameNotFoundException("Dashboard user not found");
        }
        DashboardAuthenticationUserRow base = rows.get(0);
        DashboardAuthenticationUserSnapshot snapshot = new DashboardAuthenticationUserSnapshot(
                base.userId(),
                base.email(),
                base.name(),
                base.role(),
                base.active(),
                rows.stream()
                        .map(DashboardAuthenticationUserRow::permission)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet())
        );
        if (!snapshot.active()) {
            throw new DisabledException("Dashboard user is disabled");
        }
        loads.increment();
        return snapshot;
    }

    private DashboardAuthenticationUserSnapshot loadWithFailureMetric(Long userId) {
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
