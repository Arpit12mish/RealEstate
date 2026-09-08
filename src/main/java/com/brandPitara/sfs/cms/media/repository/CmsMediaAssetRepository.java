package com.brandPitara.sfs.cms.media.repository;

import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.*;

public interface CmsMediaAssetRepository extends JpaRepository<CmsMediaAssetEntity, Long> {

    @Query("select m from CmsMediaAssetEntity m where m.id in :ids")
    List<CmsMediaAssetEntity> findAllByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = "createdBy")
    @Query("select m from CmsMediaAssetEntity m where m.id = :id")
    Optional<CmsMediaAssetEntity> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from CmsMediaAssetEntity m where m.id = :id")
    Optional<CmsMediaAssetEntity> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "createdBy")
    @Query("""
            select m from CmsMediaAssetEntity m
            where (:mediaType is null or m.mediaType = :mediaType)
              and (:status is null or m.status = :status)
              and (:createdBy is null or m.createdBy.id = :createdBy)
              and (:search is null or lower(m.originalFilename) like :search)
            """)
    Page<CmsMediaAssetEntity> findPage(
            @Param("mediaType") CmsMediaType mediaType,
            @Param("status") CmsMediaStatus status,
            @Param("createdBy") Long createdBy,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            select m.id from CmsMediaAssetEntity m
            where (m.status = com.brandPitara.sfs.cms.media.domain.CmsMediaStatus.PENDING_UPLOAD and m.createdAt < :pendingBefore)
               or (m.status = com.brandPitara.sfs.cms.media.domain.CmsMediaStatus.FAILED and m.failedAt < :failedBefore)
               or (m.status = com.brandPitara.sfs.cms.media.domain.CmsMediaStatus.DELETING and m.updatedAt < :pendingBefore)
            order by m.createdAt asc
            """)
    List<Long> findCleanupCandidates(
            @Param("pendingBefore") OffsetDateTime pendingBefore,
            @Param("failedBefore") OffsetDateTime failedBefore,
            Pageable pageable
    );
}
