package com.brandPitara.sfs.mobileupdate.cache;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.entity.MobileAppUpdatePolicyEntity;
import com.brandPitara.sfs.mobileupdate.repository.MobileAppUpdatePolicyRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MobileAppUpdatePolicyCache {

    private final MobileAppUpdatePolicyRepository repository;
    private final Cache<MobilePlatform, MobileAppUpdatePolicyEntity> cache = Caffeine.newBuilder()
            .maximumSize(MobilePlatform.values().length)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    public MobileAppUpdatePolicyCache(MobileAppUpdatePolicyRepository repository) {
        this.repository = repository;
    }

    public MobileAppUpdatePolicyEntity find(MobilePlatform platform) {
        return cache.get(platform, key -> repository.findById(key).orElse(null));
    }

    public void evict(MobilePlatform platform) {
        cache.invalidate(platform);
    }
}

