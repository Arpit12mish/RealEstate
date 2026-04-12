package com.brandPitara.sfs.builder.repository;


import com.brandPitara.sfs.builder.entity.BuilderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuilderRepository extends JpaRepository<BuilderEntity, Long> {

  Optional<BuilderEntity> findByIdAndDeletedFalse(Long id);

  Page<BuilderEntity> findByDeletedFalse(Pageable pageable);

  Page<BuilderEntity> findByPublishedAndDeletedFalse(boolean published, Pageable pageable);

  Page<BuilderEntity> findByActiveAndDeletedFalse(boolean active, Pageable pageable);

  Page<BuilderEntity> findByPublishedAndActiveAndDeletedFalse(boolean published, boolean active, Pageable pageable);

  Page<BuilderEntity> findByPublishedTrueAndActiveTrueAndDeletedFalse(Pageable pageable);

  List<BuilderEntity> findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc();

  List<BuilderEntity> findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseAndCity_IdOrderByPriorityAscIdDesc(Long cityId);
  List<BuilderEntity> findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(Collection<Long> ids);
  List<BuilderEntity> findByIdInAndActiveTrueAndDeletedFalse(Collection<Long> ids);





    @EntityGraph(attributePaths = {"city"})
  @Query("""
      select b
      from BuilderEntity b
      left join b.city c
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
        b.priority asc,
        b.id desc
      """)
  List<BuilderEntity> searchForSuggestions(
      @Param("query") String query,
      @Param("cityId") Long cityId,
      Pageable pageable
  );

  @EntityGraph(attributePaths = {"city"})
  @Query("""
      select b
      from BuilderEntity b
      left join b.city c
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
        b.priority asc,
        b.id desc
      """)
  Page<BuilderEntity> searchForResults(
      @Param("query") String query,
      @Param("cityId") Long cityId,
      Pageable pageable
  );
}
