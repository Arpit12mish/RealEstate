package com.brandPitara.sfs.dashboard.builderhighlight.progress.repository;

import com.brandPitara.sfs.builderhighlight.entity.BuilderHighlightItemEntity;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightProgressRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Read-only aggregation repository, deliberately separate from
 * {@code BuilderHighlightItemRepository} so the progress feature never touches
 * the existing item CRUD queries. Extends the plain marker {@link Repository}
 * (not JpaRepository) so no CRUD methods need implementing.
 */
public interface BuilderHighlightProgressRepository extends Repository<BuilderHighlightItemEntity, Long> {

    @Query("""
        select new com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightProgressRow(
            i.builder.id, i.builder.name, i.highlightType, i.status, i.publicVisible, i.active, i.updatedAt, i.title)
        from BuilderHighlightItemEntity i
        where i.deletedAt is null
        """)
    List<BuilderHighlightProgressRow> findAllProgressRows();

    @Query("""
        select new com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightProgressRow(
            i.builder.id, i.builder.name, i.highlightType, i.status, i.publicVisible, i.active, i.updatedAt, i.title)
        from BuilderHighlightItemEntity i
        where i.builder.id in :builderIds
          and i.deletedAt is null
        """)
    List<BuilderHighlightProgressRow> findProgressRowsByBuilderIds(@Param("builderIds") Collection<Long> builderIds);
}
