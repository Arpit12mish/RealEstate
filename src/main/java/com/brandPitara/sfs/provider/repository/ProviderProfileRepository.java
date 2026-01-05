package com.brandPitara.sfs.provider.repository;

import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfileEntity, Long> {

    Optional<ProviderProfileEntity> findByUserId(Long userId);

    @Query("""
        select distinct p
        from ProviderProfileEntity p
        join ProviderServiceAreaEntity a on a.provider = p
        where p.id <> :excludeProviderId
          and p.primaryCategory.id = :categoryId
          and a.city.id = :cityId
        order by p.featured desc, p.updatedAt desc
    """)
    List<ProviderProfileEntity> findSimilar(Long excludeProviderId, Long categoryId, Long cityId, Pageable pageable);
}
