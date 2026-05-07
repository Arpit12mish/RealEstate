package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

  Optional<ProjectEntity> findByIdAndDeletedFalse(Long id);

  Page<ProjectEntity> findByDeletedFalse(Pageable pageable);

  Page<ProjectEntity> findByBuilderIdAndDeletedFalse(Long builderId, Pageable pageable);

  // Existing method kept for backward compatibility.
  Page<ProjectEntity> findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalse(
      Long builderId,
      Pageable pageable
  );

  // New approved-only public method.
  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
      Long builderId,
      ReviewStatus reviewStatus,
      Pageable pageable
  );

  // Existing method kept for backward compatibility.
  @EntityGraph(attributePaths = {"builder", "city"})
  List<ProjectEntity> findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(
      Long builderId
  );

  // New approved-only public method.
  @EntityGraph(attributePaths = {"builder", "city"})
  List<ProjectEntity> findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatusOrderByPriorityAscIdDesc(
      Long builderId,
      ReviewStatus reviewStatus
  );

  // Existing method kept for backward compatibility.
  @EntityGraph(attributePaths = {"builder"})
  List<ProjectEntity> findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(Collection<Long> ids);

  // New approved-only public method.
  @EntityGraph(attributePaths = {"builder"})
  List<ProjectEntity> findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
      Collection<Long> ids,
      ReviewStatus reviewStatus
  );

  // Existing method kept for backward compatibility.
  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByPublishedTrueAndActiveTrueAndDeletedFalse(Pageable pageable);

  // New approved-only public method.
  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
      ReviewStatus reviewStatus,
      Pageable pageable
  );

  // Existing method kept for backward compatibility.
  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalse(
      Long cityId,
      Pageable pageable
  );

  // New approved-only public method.
  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
      Long cityId,
      ReviewStatus reviewStatus,
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
        and p.reviewStatus = com.brandPitara.sfs.dashboard.common.enums.ReviewStatus.APPROVED
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
  @Query(
      value = """
          select p
          from ProjectEntity p
          left join p.builder b
          left join p.city c
          where p.active = true
            and p.published = true
            and p.deleted = false
            and p.reviewStatus = com.brandPitara.sfs.dashboard.common.enums.ReviewStatus.APPROVED
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
          """,
      countQuery = """
          select count(distinct p.id)
          from ProjectEntity p
          left join p.builder b
          left join p.city c
          where p.active = true
            and p.published = true
            and p.deleted = false
            and p.reviewStatus = com.brandPitara.sfs.dashboard.common.enums.ReviewStatus.APPROVED
            and (:cityId is null or c.id = :cityId)
            and (
                  lower(p.name) like lower(concat(:query, '%'))
               or lower(p.name) like lower(concat('%', :query, '%'))
               or lower(b.name) like lower(concat(:query, '%'))
               or lower(b.name) like lower(concat('%', :query, '%'))
            )
          """
  )
  Page<ProjectEntity> searchForResults(
      @Param("query") String query,
      @Param("cityId") Long cityId,
      Pageable pageable
  );

  // Existing method kept for backward compatibility.
  long countByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long cityId);

  // New approved-only public count.
  long countByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
      Long cityId,
      ReviewStatus reviewStatus
  );

  Page<ProjectEntity> findByDeletedFalseAndReviewStatus(
    ReviewStatus reviewStatus,
    Pageable pageable
  );

  Page<ProjectEntity> findByBuilderIdAndDeletedFalseAndReviewStatus(
      Long builderId,
      ReviewStatus reviewStatus,
      Pageable pageable
  );

  long countByDeletedFalse();

  long countByDeletedFalseAndReviewStatus(ReviewStatus reviewStatus);

  long countByDeletedFalseAndSubmittedAtBetween(OffsetDateTime start, OffsetDateTime end);
}