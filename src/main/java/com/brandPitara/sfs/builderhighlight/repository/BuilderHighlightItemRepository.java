package com.brandPitara.sfs.builderhighlight.repository;

import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightItemEntity;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BuilderHighlightItemRepository extends JpaRepository<BuilderHighlightItemEntity, Long> {

    @EntityGraph(attributePaths = {"builder", "project", "city", "points"})
    Optional<BuilderHighlightItemEntity> findByIdAndBuilder_IdAndDeletedAtIsNull(Long id, Long builderId);

    @EntityGraph(attributePaths = {"builder", "project", "city", "points"})
    Optional<BuilderHighlightItemEntity>
    findByIdAndBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
        Long id,
        Long builderId,
        BuilderHighlightStatus status
    );

    boolean existsByBuilder_IdAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
        Long builderId,
        BuilderHighlightStatus status
    );

    @EntityGraph(attributePaths = {"builder", "project", "city"})
    @Query("""
        select i
        from BuilderHighlightItemEntity i
        where i.builder.id = :builderId
          and (:type is null or i.highlightType = :type)
          and (:status is null or i.status = :status)
          and i.deletedAt is null
        """)
    Page<BuilderHighlightItemEntity> dashboardList(
        @Param("builderId") Long builderId,
        @Param("type") BuilderHighlightType type,
        @Param("status") BuilderHighlightStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"builder", "project", "city"})
    Page<BuilderHighlightItemEntity>
    findByBuilder_IdAndHighlightTypeAndStatusAndPublicVisibleTrueAndActiveTrueAndDeletedAtIsNull(
        Long builderId,
        BuilderHighlightType highlightType,
        BuilderHighlightStatus status,
        Pageable pageable
    );
}
