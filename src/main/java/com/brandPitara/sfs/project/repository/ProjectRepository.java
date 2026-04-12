package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

  Optional<ProjectEntity> findByIdAndDeletedFalse(Long id);

  Page<ProjectEntity> findByDeletedFalse(Pageable pageable);

  Page<ProjectEntity> findByBuilderIdAndDeletedFalse(Long builderId, Pageable pageable);

  Page<ProjectEntity> findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long builderId, Pageable pageable);

  @EntityGraph(attributePaths = {"builder", "city"})
  List<ProjectEntity> findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(Long builderId);


  @EntityGraph(attributePaths = {"builder"}) // ✅ avoids lazy loading builder N+1
  List<ProjectEntity> findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(Collection<Long> ids);

  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByPublishedTrueAndActiveTrueAndDeletedFalse(Pageable pageable);

  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long cityId, Pageable pageable);



    @EntityGraph(attributePaths = {"builder", "city"})
  @Query("""
      select p
      from ProjectEntity p
      left join p.builder b
      left join p.city c
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
        p.priority asc,
        p.id desc
      """)
  List<ProjectEntity> searchForSuggestions(
      @Param("query") String query,
      @Param("cityId") Long cityId,
      Pageable pageable
  );

  @EntityGraph(attributePaths = {"builder", "city"})
  @Query("""
      select p
      from ProjectEntity p
      left join p.builder b
      left join p.city c
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
        p.priority asc,
        p.id desc
      """)
  Page<ProjectEntity> searchForResults(
      @Param("query") String query,
      @Param("cityId") Long cityId,
      Pageable pageable
  );

}
