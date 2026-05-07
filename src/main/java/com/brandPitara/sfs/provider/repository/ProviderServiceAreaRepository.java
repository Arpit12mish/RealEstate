package com.brandPitara.sfs.provider.repository;


import com.brandPitara.sfs.provider.entity.ProviderServiceAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProviderServiceAreaRepository extends JpaRepository<ProviderServiceAreaEntity, Long> {
    List<ProviderServiceAreaEntity> findByProviderId(Long providerId);
    void deleteByProviderId(Long providerId);

    @Query("select a from ProviderServiceAreaEntity a join fetch a.city where a.provider.id in :providerIds")
    List<ProviderServiceAreaEntity> findByProviderIdIn(Collection<Long> providerIds);
}


