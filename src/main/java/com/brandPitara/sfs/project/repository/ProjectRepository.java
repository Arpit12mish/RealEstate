package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

  Optional<ProjectEntity> findByIdAndDeletedFalse(Long id);

  Optional<ProjectEntity> findBySlug(String slug);

  Optional<ProjectEntity> findBySlugAndIdNot(String slug, Long id);

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

  @EntityGraph(attributePaths = {"builder", "city"})
  List<ProjectEntity> findByBuilderIdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(
      Collection<Long> builderIds
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

  // Paginated ID-IN browse (used for unit configuration filter).
  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
      Collection<Long> ids,
      ReviewStatus reviewStatus,
      Pageable pageable
  );

  // Paginated ID-IN browse filtered by city.
  @EntityGraph(attributePaths = {"builder", "city"})
  Page<ProjectEntity> findByIdInAndCityIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
      Collection<Long> ids,
      Long cityId,
      ReviewStatus reviewStatus,
      Pageable pageable
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

  @Query("""
      select count(p.id)
      from ProjectEntity p
      join p.builder b
      where p.city.id = :cityId
        and p.published = true
        and p.active = true
        and p.deleted = false
        and p.reviewStatus = com.brandPitara.sfs.dashboard.common.enums.ReviewStatus.APPROVED
        and b.published = true
        and b.active = true
        and b.deleted = false
      """)
  long countPublicProjectsByCityWithPublicBuilder(@Param("cityId") Long cityId);

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

  @EntityGraph(attributePaths = {"builder", "city"})
  @Query("""
      select p from ProjectEntity p
      where p.published = true and p.active = true and p.deleted = false
        and p.reviewStatus = :reviewStatus
        and (:cityId is null or p.city.id = :cityId)
        and exists (
          select 1 from ProjectFloorPlanEntity fp
          where fp.project = p
            and fp.unitConfigurationType in :types
            and fp.active = true and fp.deleted = false
        )
  """)
  Page<ProjectEntity> browsePublicByUnitConfiguration(
      @Param("reviewStatus") ReviewStatus reviewStatus,
      @Param("cityId") Long cityId,
      @Param("types") List<UnitConfigurationType> types,
      Pageable pageable
  );

  @Query(
      value = """
          select
            p.id as projectId,
            p.name as projectName,
            p.slug as projectSlug,
            media.cover_image_url as coverImageUrl,
            p.address_line as addressLine,
            c.name as cityName,
            b.id as builderId,
            b.name as builderName,
            b.logo_url as builderLogoUrl,
            p.price_min as priceMin,
            p.price_max as priceMax,
            p.latitude as latitude,
            p.longitude as longitude,
            (
              6371 * acos(
                least(1, greatest(-1,
                  cos(radians(:latitude)) *
                  cos(radians(p.latitude)) *
                  cos(radians(p.longitude) - radians(:longitude)) +
                  sin(radians(:latitude)) *
                  sin(radians(p.latitude))
                ))
              )
            ) as distanceKm,
            coalesce(snapshot.verified, false) as verified,
            fp.unit_configuration_type as unitConfigurationType,
            fp.unit_label as unitLabel,
            fp.bedrooms as bedrooms,
            fp.saleable_area_sqft as saleableAreaSqft,
            fp.super_area_sqft as superAreaSqft,
            fp.carpet_area_sqft as carpetAreaSqft
          from project p
          join builder b on b.id = p.builder_id
          left join city c on c.id = p.city_id
          left join project_meter_snapshot snapshot on snapshot.project_id = p.id
          left join lateral (
            select pm.url as cover_image_url
            from project_media pm
            where pm.project_id = p.id
              and pm.active = true
              and pm.deleted = false
              and pm.url is not null
              and pm.url <> ''
              and pm.media_type in ('IMAGE', 'VIDEO')
            order by case when pm.media_type = 'IMAGE' then 0 else 1 end, pm.sort_order asc, pm.id desc
            limit 1
          ) media on true
          left join lateral (
            select
              pfp.unit_configuration_type,
              pfp.unit_label,
              pfp.bedrooms,
              pfp.saleable_area_sqft,
              pfp.super_area_sqft,
              pfp.carpet_area_sqft
            from project_floor_plan pfp
            where pfp.project_id = p.id
              and pfp.active = true
              and pfp.deleted = false
            order by pfp.featured desc, pfp.sort_order asc, pfp.id asc
            limit 1
          ) fp on true
          where p.published = true
            and p.active = true
            and p.deleted = false
            and p.review_status = 'APPROVED'
            and b.published = true
            and b.active = true
            and b.deleted = false
            and (c.id is null or c.active = true)
            and p.latitude is not null
            and p.longitude is not null
          order by distanceKm asc, p.priority asc, p.id desc
          limit :limit
          """,
      nativeQuery = true
  )
  List<ProjectNearbyListingProjection> findNearbyPublicProjectCards(
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("limit") int limit
  );

  @Query(
      value = """
          select
            p.id as projectId,
            p.name as projectName,
            p.slug as projectSlug,
            media.cover_image_url as coverImageUrl,
            p.address_line as addressLine,
            c.name as cityName,
            b.id as builderId,
            b.name as builderName,
            b.logo_url as builderLogoUrl,
            p.price_min as priceMin,
            p.price_max as priceMax,
            p.latitude as latitude,
            p.longitude as longitude,
            cast(null as double precision) as distanceKm,
            coalesce(snapshot.verified, false) as verified,
            fp.unit_configuration_type as unitConfigurationType,
            fp.unit_label as unitLabel,
            fp.bedrooms as bedrooms,
            fp.saleable_area_sqft as saleableAreaSqft,
            fp.super_area_sqft as superAreaSqft,
            fp.carpet_area_sqft as carpetAreaSqft
          from project p
          join builder b on b.id = p.builder_id
          left join city c on c.id = p.city_id
          left join project_meter_snapshot snapshot on snapshot.project_id = p.id
          left join lateral (
            select pm.url as cover_image_url
            from project_media pm
            where pm.project_id = p.id
              and pm.active = true
              and pm.deleted = false
              and pm.url is not null
              and pm.url <> ''
              and pm.media_type in ('IMAGE', 'VIDEO')
            order by case when pm.media_type = 'IMAGE' then 0 else 1 end, pm.sort_order asc, pm.id desc
            limit 1
          ) media on true
          left join lateral (
            select
              pfp.unit_configuration_type,
              pfp.unit_label,
              pfp.bedrooms,
              pfp.saleable_area_sqft,
              pfp.super_area_sqft,
              pfp.carpet_area_sqft
            from project_floor_plan pfp
            where pfp.project_id = p.id
              and pfp.active = true
              and pfp.deleted = false
            order by pfp.featured desc, pfp.sort_order asc, pfp.id asc
            limit 1
          ) fp on true
          where p.published = true
            and p.active = true
            and p.deleted = false
            and p.review_status = 'APPROVED'
            and p.city_id = :cityId
            and b.published = true
            and b.active = true
            and b.deleted = false
            and (c.id is null or c.active = true)
          order by p.priority asc, p.id desc
          limit :limit
          """,
      nativeQuery = true
  )
  List<ProjectNearbyListingProjection> findPublicProjectCardsByCity(
      @Param("cityId") Long cityId,
      @Param("limit") int limit
  );

  @Query(
      value = """
          select
            p.id as projectId,
            p.name as projectName,
            p.slug as projectSlug,
            media.cover_image_url as coverImageUrl,
            p.address_line as addressLine,
            c.name as cityName,
            b.id as builderId,
            b.name as builderName,
            b.logo_url as builderLogoUrl,
            p.price_min as priceMin,
            p.price_max as priceMax,
            p.latitude as latitude,
            p.longitude as longitude,
            cast(null as double precision) as distanceKm,
            coalesce(snapshot.verified, false) as verified,
            fp.unit_configuration_type as unitConfigurationType,
            fp.unit_label as unitLabel,
            fp.bedrooms as bedrooms,
            fp.saleable_area_sqft as saleableAreaSqft,
            fp.super_area_sqft as superAreaSqft,
            fp.carpet_area_sqft as carpetAreaSqft
          from project p
          join builder b on b.id = p.builder_id
          left join city c on c.id = p.city_id
          left join project_meter_snapshot snapshot on snapshot.project_id = p.id
          left join lateral (
            select pm.url as cover_image_url
            from project_media pm
            where pm.project_id = p.id
              and pm.active = true
              and pm.deleted = false
              and pm.url is not null
              and pm.url <> ''
              and pm.media_type in ('IMAGE', 'VIDEO')
            order by case when pm.media_type = 'IMAGE' then 0 else 1 end, pm.sort_order asc, pm.id desc
            limit 1
          ) media on true
          left join lateral (
            select
              pfp.unit_configuration_type,
              pfp.unit_label,
              pfp.bedrooms,
              pfp.saleable_area_sqft,
              pfp.super_area_sqft,
              pfp.carpet_area_sqft
            from project_floor_plan pfp
            where pfp.project_id = p.id
              and pfp.active = true
              and pfp.deleted = false
            order by pfp.featured desc, pfp.sort_order asc, pfp.id asc
            limit 1
          ) fp on true
          where p.published = true
            and p.active = true
            and p.deleted = false
            and p.review_status = 'APPROVED'
            and b.published = true
            and b.active = true
            and b.deleted = false
            and (c.id is null or c.active = true)
          order by p.priority asc, p.id desc
          limit :limit
          """,
      nativeQuery = true
  )
  List<ProjectNearbyListingProjection> findFallbackPublicProjectCards(@Param("limit") int limit);
}
