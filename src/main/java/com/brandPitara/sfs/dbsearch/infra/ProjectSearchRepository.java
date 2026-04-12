package com.brandPitara.sfs.dbsearch.infra;

import com.brandPitara.sfs.project.entity.ProjectEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectSearchRepository extends Repository<ProjectEntity, Long> {

    @Query("""
        select p
        from ProjectEntity p
        left join fetch p.builder b
        left join fetch p.city c
        where p.active = true
          and p.published = true
          and p.deleted = false
          and (:cityId is null or c.id = :cityId)
          and (
                lower(p.name) like lower(concat(:query, '%'))
             or lower(p.name) like lower(concat('%', :query, '%'))
             or lower(b.name) like lower(concat(:query, '%'))
             or lower(b.name) like lower(concat('%', :query, '%'))
          )
        order by
          case
            when lower(p.name) = lower(:query) then 1
            when lower(p.name) like lower(concat(:query, '%')) then 2
            when lower(b.name) = lower(:query) then 3
            when lower(b.name) like lower(concat(:query, '%')) then 4
            when lower(p.name) like lower(concat('%', :query, '%')) then 5
            when lower(b.name) like lower(concat('%', :query, '%')) then 6
            else 7
          end,
          p.priority desc,
          p.updatedAt desc
        """)
    List<ProjectEntity> searchProjects(
            @Param("query") String query,
            @Param("cityId") Long cityId,
            Pageable pageable
    );
}