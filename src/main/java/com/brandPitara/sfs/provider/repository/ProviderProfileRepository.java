package com.brandPitara.sfs.provider.repository;

import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfileEntity, Long> {

    Optional<ProviderProfileEntity> findByUserId(Long userId);

    /** Public worker profile: VERIFIED WORKER whose linked listing (if any) is active. */
    @Query("""
        select p from ProviderProfileEntity p
        join fetch p.primaryCategory
        left join fetch p.business b
        where p.id = :id
          and p.providerType = com.brandPitara.sfs.provider.enums.ProviderType.WORKER
          and p.verificationStatus = com.brandPitara.sfs.provider.enums.VerificationStatus.VERIFIED
          and (b is null or b.active = true)
    """)
    Optional<ProviderProfileEntity> findPublicWorker(@Param("id") Long id);

    @Query("""
        select p from ProviderProfileEntity p
        join fetch p.primaryCategory
        left join fetch p.business
        where p.id = :id
          and p.providerType = com.brandPitara.sfs.provider.enums.ProviderType.WORKER
    """)
    Optional<ProviderProfileEntity> findWorkerForManagement(@Param("id") Long id);

    @Query(value = """
        select p from ProviderProfileEntity p
        join fetch p.primaryCategory
        where p.providerType = com.brandPitara.sfs.provider.enums.ProviderType.WORKER
          and (:q = '' or lower(p.displayName) like lower(concat('%', :q, '%')))
        order by p.id desc
    """, countQuery = """
        select count(p) from ProviderProfileEntity p
        where p.providerType = com.brandPitara.sfs.provider.enums.ProviderType.WORKER
          and (:q = '' or lower(p.displayName) like lower(concat('%', :q, '%')))
    """)
    Page<ProviderProfileEntity> searchWorkersForManagement(
            @Param("q") String q, Pageable pageable);

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
