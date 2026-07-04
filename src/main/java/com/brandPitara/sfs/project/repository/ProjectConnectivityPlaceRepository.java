package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectConnectivityPlaceEntity;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectConnectivityPlaceRepository extends JpaRepository<ProjectConnectivityPlaceEntity, Long> {

  List<ProjectConnectivityPlaceEntity> findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  List<ProjectConnectivityPlaceEntity> findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  List<ProjectConnectivityPlaceEntity> findByProjectIdInAndActiveTrueAndDeletedFalseOrderByProjectIdAscCategoryAscSortOrderAscIdAsc(Collection<Long> projectIds);

  Optional<ProjectConnectivityPlaceEntity> findByIdAndProjectIdAndDeletedFalse(Long id, Long projectId);

  List<ProjectConnectivityPlaceEntity>
  findByProjectIdAndCategoryAndActiveTrueAndDeletedFalseOrderByFeaturedDescSortOrderAscDistanceMetersAscIdAsc(
      Long projectId,
      ProjectConnectivityCategory category
  );

  @Query("""
    select p
    from ProjectConnectivityPlaceEntity p
    where p.project.id = :projectId
      and p.active = true
      and p.deleted = false
      and (
        lower(p.placeName) like lower(concat('%', :query, '%'))
        or lower(coalesce(p.address, '')) like lower(concat('%', :query, '%'))
      )
    order by
      p.featured desc,
      p.sortOrder asc,
      p.id asc
  """)
  List<ProjectConnectivityPlaceEntity> searchSavedPlaces(
      @Param("projectId") Long projectId,
      @Param("query") String query,
      Pageable pageable
  );

  // --- Duplicate detection ---

  Optional<ProjectConnectivityPlaceEntity> findByProjectIdAndProviderAndExternalPlaceIdAndDeletedFalse(
      Long projectId,
      String provider,
      String externalPlaceId
  );

  boolean existsByProjectIdAndProviderAndExternalPlaceIdAndDeletedFalse(
      Long projectId,
      String provider,
      String externalPlaceId
  );

  @Query("""
    select count(p) > 0
    from ProjectConnectivityPlaceEntity p
    where p.project.id = :projectId
      and p.deleted = false
      and lower(p.placeName) = lower(:placeName)
      and p.placeType = :placeType
  """)
  boolean existsDuplicateByNameAndType(
      @Param("projectId") Long projectId,
      @Param("placeName") String placeName,
      @Param("placeType") ProjectConnectivityType placeType
  );

  @Query("select max(p.sortOrder) from ProjectConnectivityPlaceEntity p where p.project.id = :projectId and p.deleted = false")
  Optional<Integer> findMaxSortOrderByProjectId(@Param("projectId") Long projectId);

  // Returns "provider::externalPlaceId" composite keys for all saved places that have an externalPlaceId.
  @Query("""
    select concat(coalesce(p.provider, ''), '::', p.externalPlaceId)
    from ProjectConnectivityPlaceEntity p
    where p.project.id = :projectId
      and p.externalPlaceId is not null
      and p.deleted = false
  """)
  Set<String> findAllProviderExternalIdKeysByProjectId(@Param("projectId") Long projectId);

  // Returns "lower(placeName)::placeType" composite keys for name+type duplicate detection.
  @Query("""
    select concat(lower(p.placeName), '::', cast(p.placeType as string))
    from ProjectConnectivityPlaceEntity p
    where p.project.id = :projectId
      and p.placeName is not null
      and p.placeType is not null
      and p.deleted = false
  """)
  Set<String> findAllNameTypeKeysByProjectId(@Param("projectId") Long projectId);

  // Returns "lower(placeName)::category" composite keys for fallback duplicate detection.
  @Query("""
    select concat(lower(p.placeName), '::', cast(p.category as string))
    from ProjectConnectivityPlaceEntity p
    where p.project.id = :projectId
      and p.placeName is not null
      and p.category is not null
      and p.deleted = false
  """)
  Set<String> findAllNameCategoryKeysByProjectId(@Param("projectId") Long projectId);
}
