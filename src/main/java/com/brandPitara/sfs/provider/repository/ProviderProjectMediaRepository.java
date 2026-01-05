package com.brandPitara.sfs.provider.repository;

import com.brandPitara.sfs.provider.entity.ProviderProjectMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderProjectMediaRepository extends JpaRepository<ProviderProjectMediaEntity, Long> {
    // Not strictly needed because you have cascade + orphanRemoval,
    // but keeping repo is useful for future admin features.
}
