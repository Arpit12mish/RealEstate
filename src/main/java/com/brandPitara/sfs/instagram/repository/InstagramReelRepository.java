package com.brandPitara.sfs.instagram.repository;

import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface InstagramReelRepository extends JpaRepository<InstagramReelEntity, Long> {

    Optional<InstagramReelEntity> findByIdAndDeletedFalse(Long id);

    Optional<InstagramReelEntity> findByInstagramMediaIdAndDeletedFalse(String instagramMediaId);

    boolean existsByInstagramUrlAndDeletedFalse(String instagramUrl);

    boolean existsByInstagramUrlAndDeletedFalseAndIdNot(String instagramUrl, Long id);

    Page<InstagramReelEntity> findByActiveTrueAndDeletedFalseOrderByPublishedAtDescIdDesc(Pageable pageable);

    Page<InstagramReelEntity> findByActiveTrueAndDeletedFalseOrderByTrendingScoreDescPublishedAtDescIdDesc(Pageable pageable);

    Page<InstagramReelEntity> findByActiveTrueAndDeletedFalseOrderByViewCountDescPublishedAtDescIdDesc(Pageable pageable);

    Page<InstagramReelEntity> findByActiveTrueAndDeletedFalseAndCategoryOverrideOrderByDisplayOrderAscPublishedAtDescIdDesc(
        InstagramReelCategory categoryOverride,
        Pageable pageable
    );

    List<InstagramReelEntity> findByDeletedFalse();

    List<InstagramReelEntity> findByActiveTrueAndDeletedFalseAndCachedThumbnailUrlIsNull();

    @Query("""
        select r
        from InstagramReelEntity r
        where r.deleted = false
          and (:active is null or r.active = :active)
          and (:category is null or r.categoryOverride = :category)
        """)
    Page<InstagramReelEntity> dashboardList(
        @Param("active") Boolean active,
        @Param("category") InstagramReelCategory category,
        Pageable pageable
    );

    @Query("""
        select r
        from InstagramReelEntity r
        where r.deleted = false
          and (:active is null or r.active = :active)
          and (:category is null or r.categoryOverride = :category)
          and (
            lower(coalesce(r.title, '')) like :queryPattern
            or lower(coalesce(r.caption, '')) like :queryPattern
            or lower(coalesce(r.instagramUrl, '')) like :queryPattern
          )
        """)
    Page<InstagramReelEntity> dashboardSearch(
        @Param("active") Boolean active,
        @Param("category") InstagramReelCategory category,
        @Param("queryPattern") String queryPattern,
        Pageable pageable
    );
}
