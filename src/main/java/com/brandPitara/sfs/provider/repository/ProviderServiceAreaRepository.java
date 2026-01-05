package com.brandPitara.sfs.provider.repository;


import com.brandPitara.sfs.provider.entity.ProviderServiceAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderServiceAreaRepository extends JpaRepository<ProviderServiceAreaEntity, Long> {
    List<ProviderServiceAreaEntity> findByProviderId(Long providerId);
    void deleteByProviderId(Long providerId);
}


