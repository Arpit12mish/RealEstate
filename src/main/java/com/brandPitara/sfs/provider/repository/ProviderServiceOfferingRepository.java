package com.brandPitara.sfs.provider.repository;

import com.brandPitara.sfs.provider.entity.ProviderServiceOfferingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProviderServiceOfferingRepository extends JpaRepository<ProviderServiceOfferingEntity, Long> {

    List<ProviderServiceOfferingEntity> findByProvider_IdOrderBySortOrderAscIdAsc(Long providerId);

    @Modifying
    @Query("delete from ProviderServiceOfferingEntity s where s.provider.id = :providerId")
    void deleteByProviderId(@Param("providerId") Long providerId);
}
