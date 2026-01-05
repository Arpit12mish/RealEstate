package com.brandPitara.sfs.provider.repository;

import com.brandPitara.sfs.provider.entity.ProviderMediaEntity;
import com.brandPitara.sfs.provider.enums.ProviderMediaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderMediaRepository extends JpaRepository<ProviderMediaEntity, Long> {

    List<ProviderMediaEntity> findByProviderIdOrderBySortOrderAsc(Long providerId);

    long countByProviderIdAndMediaType(Long providerId, ProviderMediaType type);

    Optional<ProviderMediaEntity> findFirstByProviderIdAndMediaType(Long providerId, ProviderMediaType type);

    void deleteByProviderIdAndMediaType(Long providerId, ProviderMediaType type);
}
