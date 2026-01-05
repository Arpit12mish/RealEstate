package com.brandPitara.sfs.provider.repository;

import com.brandPitara.sfs.provider.entity.ProviderProjectEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderProjectRepository extends JpaRepository<ProviderProjectEntity, Long> {

    // List projects for a provider (latest first) — since you don't have createdAt in entity, order by id desc.
    @EntityGraph(attributePaths = {"category", "city", "media"})
    List<ProviderProjectEntity> findByProviderIdOrderByIdDesc(Long providerId);

    // Secure delete: only allow project that belongs to provider
    @EntityGraph(attributePaths = {"media"})
    Optional<ProviderProjectEntity> findByIdAndProviderId(Long id, Long providerId);

    long countByProviderId(Long providerId);
}
