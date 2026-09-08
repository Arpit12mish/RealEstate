package com.brandPitara.sfs.mobileupdate.cache;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.entity.MobileAppUpdatePolicyEntity;
import com.brandPitara.sfs.mobileupdate.repository.MobileAppUpdatePolicyRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MobileAppUpdatePolicyCacheTest {

    @Test
    void cachesByPlatformAndEvictsImmediatelyAfterUpdate() {
        MobileAppUpdatePolicyRepository repository = mock(MobileAppUpdatePolicyRepository.class);
        MobileAppUpdatePolicyEntity first = MobileAppUpdatePolicyEntity.builder()
                .platform(MobilePlatform.ANDROID).latestBuild(20L).build();
        MobileAppUpdatePolicyEntity updated = MobileAppUpdatePolicyEntity.builder()
                .platform(MobilePlatform.ANDROID).latestBuild(21L).build();
        when(repository.findById(MobilePlatform.ANDROID))
                .thenReturn(Optional.of(first), Optional.of(updated));
        MobileAppUpdatePolicyCache cache = new MobileAppUpdatePolicyCache(repository);

        assertThat(cache.find(MobilePlatform.ANDROID).getLatestBuild()).isEqualTo(20L);
        assertThat(cache.find(MobilePlatform.ANDROID).getLatestBuild()).isEqualTo(20L);
        verify(repository, times(1)).findById(MobilePlatform.ANDROID);

        cache.evict(MobilePlatform.ANDROID);
        assertThat(cache.find(MobilePlatform.ANDROID).getLatestBuild()).isEqualTo(21L);
        verify(repository, times(2)).findById(MobilePlatform.ANDROID);
    }
}
