package com.brandPitara.sfs.provider.repository;

import com.brandPitara.sfs.provider.entity.ProviderRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProviderRateRepository extends JpaRepository<ProviderRateEntity, Long> {

    @Query("""
            select r from ProviderRateEntity r
            where r.provider.id in :providerIds
            order by r.provider.id asc, r.sortOrder asc, r.id asc
            """)
    List<ProviderRateEntity> findByProviderIds(@Param("providerIds") Collection<Long> providerIds);

    @Modifying
    @Query("delete from ProviderRateEntity r where r.provider.id = :providerId")
    void deleteByProviderId(@Param("providerId") Long providerId);
}
