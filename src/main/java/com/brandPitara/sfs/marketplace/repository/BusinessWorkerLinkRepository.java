package com.brandPitara.sfs.marketplace.repository;

import com.brandPitara.sfs.marketplace.entity.BusinessWorkerLinkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusinessWorkerLinkRepository extends JpaRepository<BusinessWorkerLinkEntity, Long> {

    /** A worker is publicly listed only when it is a VERIFIED WORKER whose listing is not deactivated. */
    // Leading space: text blocks strip the trailing space of the "and " they are appended to.
    String PUBLIC_WORKER = " " + """
            p.providerType = com.brandPitara.sfs.provider.enums.ProviderType.WORKER
            and p.verificationStatus = com.brandPitara.sfs.provider.enums.VerificationStatus.VERIFIED
            and (wb is null or wb.active = true)
            """;

    @Query(value = """
            select l from BusinessWorkerLinkEntity l
            join fetch l.provider p
            join fetch p.primaryCategory
            left join fetch p.business wb
            where l.business.id = :businessId
              and l.status = com.brandPitara.sfs.marketplace.enums.WorkerLinkStatus.ACTIVE
              and """ + PUBLIC_WORKER + """
            order by l.sortOrder asc, l.id asc
            """,
            countQuery = """
            select count(l) from BusinessWorkerLinkEntity l
            join l.provider p
            left join p.business wb
            where l.business.id = :businessId
              and l.status = com.brandPitara.sfs.marketplace.enums.WorkerLinkStatus.ACTIVE
              and """ + PUBLIC_WORKER)
    Page<BusinessWorkerLinkEntity> findPublicConnectedWorkers(@Param("businessId") Long businessId, Pageable pageable);

    /** VERIFIED recommendations from active dealer stores (never from another worker's listing). */
    @Query(value = """
            select l from BusinessWorkerLinkEntity l
            join fetch l.business b
            join fetch b.city
            where l.provider.id = :providerId
              and l.status = com.brandPitara.sfs.marketplace.enums.WorkerLinkStatus.ACTIVE
              and l.recommendationStatus = com.brandPitara.sfs.marketplace.enums.WorkerRecommendationStatus.VERIFIED
              and b.active = true
            order by l.recommendationReviewedAt desc, l.id asc
            """,
            countQuery = """
            select count(l) from BusinessWorkerLinkEntity l
            join l.business b
            where l.provider.id = :providerId
              and l.status = com.brandPitara.sfs.marketplace.enums.WorkerLinkStatus.ACTIVE
              and l.recommendationStatus = com.brandPitara.sfs.marketplace.enums.WorkerRecommendationStatus.VERIFIED
              and b.active = true
            """)
    Page<BusinessWorkerLinkEntity> findVerifiedRecommendations(@Param("providerId") Long providerId, Pageable pageable);

    @Query("""
            select l from BusinessWorkerLinkEntity l
            join fetch l.provider p
            join fetch p.primaryCategory
            where l.business.id = :businessId
            order by l.sortOrder asc, l.id asc
            """)
    List<BusinessWorkerLinkEntity> findAllForManagement(@Param("businessId") Long businessId);

    Optional<BusinessWorkerLinkEntity> findByIdAndBusiness_Id(Long id, Long businessId);

    boolean existsByBusiness_IdAndProvider_Id(Long businessId, Long providerId);
}
