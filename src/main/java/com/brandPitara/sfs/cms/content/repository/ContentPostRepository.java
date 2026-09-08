package com.brandPitara.sfs.cms.content.repository;

import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContentPostRepository extends JpaRepository<ContentPostEntity, Long>,
        JpaSpecificationExecutor<ContentPostEntity> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"contentOwner", "createdBy", "updatedBy", "publicAuthor", "category", "tags", "coverMediaAsset"})
    @Query("select post from ContentPostEntity post where post.id = :contentId")
    Optional<ContentPostEntity> findDetailedById(@Param("contentId") Long contentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select post from ContentPostEntity post where post.id = :contentId")
    Optional<ContentPostEntity> findDetailedByIdForUpdate(@Param("contentId") Long contentId);

    @Query(
            value = """
                    select post.id as id,
                           post.contentType as contentType,
                           post.status as status,
                           post.title as title,
                           post.slug as slug,
                           post.excerpt as excerpt,
                           owner.id as contentOwnerDashboardUserId,
                           owner.name as contentOwnerDisplayName,
                           updater.id as updatedByDashboardUserId,
                           updater.name as updatedByDisplayName,
                           post.createdAt as createdAt,
                           post.updatedAt as updatedAt,
                           post.version as version
                    from ContentPostEntity post
                    join post.contentOwner owner
                    join post.updatedBy updater
                    where (:contentType is null or post.contentType = :contentType)
                      and (:status is null or post.status = :status)
                      and (:ownerId is null or owner.id = :ownerId)
                      and (:searchPattern is null
                           or lower(post.title) like :searchPattern escape '\\'
                           or lower(post.slug) like :searchPattern escape '\\'
                           or lower(post.excerpt) like :searchPattern escape '\\')
                    """,
            countQuery = """
                    select count(post.id)
                    from ContentPostEntity post
                    join post.contentOwner owner
                    where (:contentType is null or post.contentType = :contentType)
                      and (:status is null or post.status = :status)
                      and (:ownerId is null or owner.id = :ownerId)
                      and (:searchPattern is null
                           or lower(post.title) like :searchPattern escape '\\'
                           or lower(post.slug) like :searchPattern escape '\\'
                           or lower(post.excerpt) like :searchPattern escape '\\')
                    """
    )
    Page<ContentPostListView> findList(
            @Param("contentType") com.brandPitara.sfs.cms.content.domain.ContentType contentType,
            @Param("status") com.brandPitara.sfs.cms.content.domain.ContentStatus status,
            @Param("ownerId") Long ownerId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );
}
