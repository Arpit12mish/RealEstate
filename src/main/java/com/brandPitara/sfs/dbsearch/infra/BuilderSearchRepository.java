package com.brandPitara.sfs.dbsearch.infra;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BuilderSearchRepository extends Repository<BuilderEntity, Long> {

    @Query("""
        select b
        from BuilderEntity b
        left join fetch b.city c
        where b.active = true
          and b.published = true
          and b.deleted = false
          and (:cityId is null or c.id = :cityId)
          and (
                lower(b.name) like lower(concat(:query, '%'))
             or lower(b.name) like lower(concat('%', :query, '%'))
          )
        order by
          case
            when lower(b.name) = lower(:query) then 1
            when lower(b.name) like lower(concat(:query, '%')) then 2
            when lower(b.name) like lower(concat('%', :query, '%')) then 3
            else 4
          end,
          b.priority desc,
          b.updatedAt desc
        """)
    List<BuilderEntity> searchBuilders(
            @Param("query") String query,
            @Param("cityId") Long cityId,
            Pageable pageable
    );
}